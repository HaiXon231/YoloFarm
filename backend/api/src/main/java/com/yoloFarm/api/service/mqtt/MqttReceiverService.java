package com.yoloFarm.api.service.mqtt;

import com.yoloFarm.api.dto.SensorData;
import com.yoloFarm.api.entity.Device;
import com.yoloFarm.api.repository.DeviceRepository;
import com.yoloFarm.api.service.mqtt.observer.Observer;
import com.yoloFarm.api.service.mqtt.observer.Subject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
@RequiredArgsConstructor
public class MqttReceiverService implements Subject, MqttCallbackExtended {

    private final List<Observer> injectedObservers;
    private final List<Observer> observers = new ArrayList<>();

    private final IMqttClient mqttClient;
    private final DeviceRepository deviceRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
    private final AtomicBoolean subscribed = new AtomicBoolean(false);

    private static final int OBSERVER_CORE_THREADS = 4;
    private static final int OBSERVER_MAX_THREADS = 8;
    private static final int OBSERVER_QUEUE_CAPACITY = 1000;
    private static final AtomicInteger OBSERVER_THREAD_COUNTER = new AtomicInteger(1);

    private final java.util.concurrent.ThreadPoolExecutor observerExecutor = new java.util.concurrent.ThreadPoolExecutor(
            OBSERVER_CORE_THREADS,
            OBSERVER_MAX_THREADS,
            60L,
            java.util.concurrent.TimeUnit.SECONDS,
            new java.util.concurrent.LinkedBlockingQueue<>(OBSERVER_QUEUE_CAPACITY),
            runnable -> {
                Thread thread = new Thread(runnable,
                        "mqtt-observer-" + OBSERVER_THREAD_COUNTER.getAndIncrement());
                thread.setDaemon(true);
                return thread;
            },
            new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());

    // Cache feed key → Device để tránh query DB lặp lại mỗi MQTT message
    // ConcurrentHashMap an toàn cho multi-thread (MQTT callback + ApproveDevice
    // concurrent)
    private final java.util.concurrent.ConcurrentHashMap<String, Device> feedKeyCache = new java.util.concurrent.ConcurrentHashMap<>();

    @Value("${adafruit.mqtt.username}")
    private String username;

    @PostConstruct
    public void init() {
        if (injectedObservers != null) {
            injectedObservers.forEach(this::attach);
            log.info("MqttReceiver: Auto-attached {} observers", injectedObservers.size());
        }
        // BUG-06: Đăng ký MqttCallback để nhận sự kiện connectionLost/reconnect
        mqttClient.setCallback(this);
    }

    // ── MqttCallback implementations (BUG-06) ────────────────────────────────

    /**
     * BUG-06: Được Paho gọi khi mất kết nối broker.
     * Reset subscribed flag để subscribeIfConnected() sẽ re-subscribe sau khi reconnect.
     */
    @Override
    public void connectionLost(Throwable cause) {
        log.warn("MqttReceiver: Lost connection to Adafruit broker. Will re-subscribe after reconnect.", cause);
        subscribed.set(false);
    }

    /**
     * BUG-06: Được Paho gọi sau khi auto-reconnect thành công.
     * Re-subscribe wildcard topic để tiếp tục nhận telemetry.
     */
    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        if (reconnect) {
            log.info("MqttReceiver: Reconnected to broker [{}]. Re-subscribing...", serverURI);
            subscribeIfConnected();
        }
    }

    /** BUG-06: MqttCallback.messageArrived — delegate tới logic handler thực tế. */
    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        processIncomingMessage(topic, message);
    }

    /** Không dùng (MqttReceiverService không publish). */
    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // No-op
    }

    public void subscribeIfConnected() {
        if (subscribed.get()) {
            return;
        }
        try {
            if (!mqttClient.isConnected()) {
                log.warn("MqttReceiver: MQTT client not yet connected, deferring subscribe.");
                return;
            }

            // Subscribe wildcard bắt mọi luồng feed của tài khoản Adafruit này
            String wildcardTopic = username + "/feeds/+";
            mqttClient.subscribe(wildcardTopic, this::processIncomingMessage);
            subscribed.set(true);
            log.info("MqttReceiver: Subscribed successfully to topic: [{}]", wildcardTopic);
        } catch (Exception e) {
            log.error("MqttReceiver: Failed to subscribe to Adafruit topic", e);
        }
    }

    @Override
    public void attach(Observer o) {
        if (!observers.contains(o)) {
            observers.add(o);
        }
    }

    @Override
    public void detach(Observer o) {
        observers.remove(o);
    }

    @Override
    public void notifyObservers(SensorData data) {
        for (Observer observer : observers) {
            // Chạy observer trên bounded executor để tránh bùng nổ task ở common pool.
            try {
                observerExecutor.execute(() -> {
                    try {
                        observer.update(data);
                    } catch (Exception e) {
                        log.error("MqttReceiver: Observer [{}] threw an error while processing SensorData",
                                observer.getClass().getSimpleName(), e);
                    }
                });
            } catch (java.util.concurrent.RejectedExecutionException ex) {
                log.warn("MqttReceiver: Observer queue full, falling back to callback thread.");
                try {
                    observer.update(data);
                } catch (Exception e) {
                    log.error("MqttReceiver: Observer [{}] threw an error while processing SensorData",
                            observer.getClass().getSimpleName(), e);
                }
            }
        }
    }

    @PreDestroy
    public void shutdownObserverExecutor() {
        observerExecutor.shutdown();
    }

    private void processIncomingMessage(String topic, MqttMessage message) {
        try {
            String payload = new String(message.getPayload());

            // Xé nhỏ chuỗi Topic: "USERNAME/feeds/feedKey"
            String[] parts = topic.split("/");
            if (parts.length < 3) {
                log.warn("MqttReceiver: Invalid topic '{}', ignoring message.", topic);
                return;
            }
            String feedKey = parts[parts.length - 1]; // Lấy mảnh đuôi cùng

            log.debug("MqttReceiver: Received data [{}] from feed [{}]", payload, feedKey);

            // Tra soát ID Thiết bị theo feed key và các alias phổ biến từ Adafruit.
            Optional<Device> deviceOpt = findDeviceByFeedAlias(feedKey);

            if (deviceOpt.isPresent()) {
                Device device = deviceOpt.get();
                Float metricValue = Float.parseFloat(payload);

                // Phát hiện chuyển trạng thái OFFLINE → ONLINE để push WS (chỉ khi thực sự thay
                // đổi)
                boolean wasOffline = device.getConnectionStatus() != com.yoloFarm.api.enums.ConnectionStatusEnum.ONLINE;

                // Thay vì dùng deviceRepository.save(device) làm dính trấu Hibernate Detached
                // LazyException
                // Ta chọc thẳng UPDATE Query cực nhanh và an toàn tuyệt đối.
                device.setConnectionStatus(com.yoloFarm.api.enums.ConnectionStatusEnum.ONLINE);
                device.setLastSeen(java.time.LocalDateTime.now());
                jdbcTemplate.update("UPDATE devices SET connection_status = 'ONLINE', last_seen = ? WHERE id = ?",
                        java.sql.Timestamp.valueOf(device.getLastSeen()), device.getId());

                // Push WebSocket event khi thiết bị vừa quay lại ONLINE
                if (wasOffline) {
                    java.util.List<java.util.Map<String, Object>> onlinePayload = java.util.List
                            .of(java.util.Map.<String, Object>of(
                                    "deviceId", device.getId().toString(),
                                    "connectionStatus", "ONLINE"));
                    messagingTemplate.convertAndSend(
                            "/topic/farm/" + device.getFarm().getId() + "/device-status",
                            (Object) onlinePayload);
                    messagingTemplate.convertAndSend("/topic/admin/stats-changed",
                            (Object) java.util.Map.of("reason", "device_online"));
                    log.info("MqttReceiver: Device [{}] came back ONLINE.", device.getId());
                }

                String metricType = device.getModel().getMetricType().name();

                // Threshold validation: log warning when value is outside configured range.
                // Non-blocking — data still flows to all observers.
                Float minVal = device.getMinValue();
                Float maxVal = device.getMaxValue();
                if (minVal != null && metricValue < minVal) {
                    log.warn("MqttReceiver: Value {:.2f} from device {} is below minimum threshold {:.2f} (metric={})",
                            metricValue, device.getId(), minVal, metricType);
                }
                if (maxVal != null && metricValue > maxVal) {
                    log.warn("MqttReceiver: Value {:.2f} from device {} exceeds maximum threshold {:.2f} (metric={})",
                            metricValue, device.getId(), maxVal, metricType);
                }

                SensorData sensorData = new SensorData(
                        device.getFarm().getId(),
                        device.getId(),
                        metricType,
                        metricValue,
                        Instant.now());

                notifyObservers(sensorData);
            } else {
                log.debug("MqttReceiver: FeedKey '{}' is not registered to any farm in DB!", feedKey);
            }

        } catch (NumberFormatException nfe) {
            log.debug("MqttReceiver: Payload from Adafruit is not a number — message ignored.");
        } catch (Exception e) {
            log.error("MqttReceiver: Error processing Adafruit message", e);
        }
    }

    private Optional<Device> findDeviceByFeedAlias(String rawFeedKey) {
        // 1. Kiểm tra cache ngay (O(1)) — tránh query DB mỗi message
        if (rawFeedKey != null) {
            Device cached = feedKeyCache.get(rawFeedKey.toLowerCase(Locale.ROOT));
            if (cached != null) {
                return Optional.of(cached);
            }
        }

        // 2. Cache miss: thử các alias và query DB
        for (String candidate : buildFeedKeyCandidates(rawFeedKey)) {
            Optional<Device> deviceOpt = deviceRepository.findByAdafruitFeedKeyIgnoreCaseWithModelAndFarm(candidate);
            if (deviceOpt.isPresent()) {
                if (!candidate.equals(rawFeedKey)) {
                    log.info("MqttReceiver: Feed alias '{}' mapped to key '{}'.", rawFeedKey, candidate);
                }
                // Warm cache với key gốc và key chuẩn hóa
                Device device = deviceOpt.get();
                feedKeyCache.put(candidate.toLowerCase(Locale.ROOT), device);
                if (rawFeedKey != null) {
                    feedKeyCache.put(rawFeedKey.toLowerCase(Locale.ROOT), device);
                }
                return deviceOpt;
            }
        }
        return Optional.empty();
    }

    /**
     * Warm cache khi Device mới được approve — gọi từ DeviceService.approveDevice()
     * Tránh cache miss lần đầu khi thiết bị gửi message ngay sau khi được duyệt.
     */
    public void cacheFeedKey(String feedKey, Device device) {
        if (feedKey != null && device != null) {
            feedKeyCache.put(feedKey.toLowerCase(Locale.ROOT), device);
            log.debug("MqttReceiver: Pre-warmed cache for feed key [{}] -> device [{}]", feedKey, device.getId());
        }
    }

    /**
     * Xóa cache khi device bị remove hoặc feed key thay đổi.
     */
    public void evictFeedKeyCache(String feedKey) {
        if (feedKey != null) {
            feedKeyCache.remove(feedKey.toLowerCase(Locale.ROOT));
            log.debug("MqttReceiver: Evicted cache for feed key [{}]", feedKey);
        }
    }

    private Set<String> buildFeedKeyCandidates(String rawFeedKey) {
        Set<String> candidates = new LinkedHashSet<>();
        if (rawFeedKey == null) {
            return candidates;
        }

        String trimmed = rawFeedKey.trim();
        if (trimmed.isEmpty()) {
            return candidates;
        }

        candidates.add(trimmed);
        candidates.add(trimmed.toLowerCase(Locale.ROOT));

        if (trimmed.contains("-")) {
            String underscored = trimmed.replace('-', '_');
            candidates.add(underscored);
            candidates.add(underscored.toLowerCase(Locale.ROOT));
        }

        if (trimmed.contains("_")) {
            String dashed = trimmed.replace('_', '-');
            candidates.add(dashed);
            candidates.add(dashed.toLowerCase(Locale.ROOT));
        }

        return candidates;
    }
}
