package com.yoloFarm.api.service.mqtt;

import com.yoloFarm.api.dto.SensorData;
import com.yoloFarm.api.entity.Device;
import com.yoloFarm.api.repository.DeviceRepository;
import com.yoloFarm.api.service.mqtt.observer.Observer;
import com.yoloFarm.api.service.mqtt.observer.Subject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
@RequiredArgsConstructor
public class MqttReceiverService implements Subject {

    private final List<Observer> injectedObservers;
    private final List<Observer> observers = new ArrayList<>();

    private final IMqttClient mqttClient;
    private final DeviceRepository deviceRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final AtomicBoolean subscribed = new AtomicBoolean(false);

    // Cache feed key → Device để tránh query DB lặp lại mỗi MQTT message
    // ConcurrentHashMap an toàn cho multi-thread (MQTT callback + ApproveDevice concurrent)
    private final java.util.concurrent.ConcurrentHashMap<String, Device> feedKeyCache
            = new java.util.concurrent.ConcurrentHashMap<>();

    @Value("${adafruit.mqtt.username}")
    private String username;

    @PostConstruct
    public void init() {
        if (injectedObservers != null) {
            injectedObservers.forEach(this::attach);
            log.info("MqttReceiver: Tự động attach {} observers", injectedObservers.size());
        }
    }

    public void subscribeIfConnected() {
        if (subscribed.get()) {
            return;
        }
        try {
            if (!mqttClient.isConnected()) {
                log.warn("MqttReceiver: MQTT client chưa kết nối, tạm hoãn subscribe.");
                return;
            }

            // Subscribe wildcard bắt mọi luồng feed của tài khoản Adafruit này
            String wildcardTopic = username + "/feeds/+";
            mqttClient.subscribe(wildcardTopic, this::messageArrived);
            subscribed.set(true);
            log.info("MqttReceiver: Đã Subscribe thành công topic: [{}]", wildcardTopic);
        } catch (Exception e) {
            log.error("MqttReceiver: Không thể Subscribe topic Adafruit", e);
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
            // Mỗi observer chạy trên thread riêng của JVM thread pool
            // MQTT callback thread được giải phóng ngay lập tức
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                try {
                    observer.update(data);
                } catch (Exception e) {
                    log.error("MqttReceiver: Observer [{}] gặp lỗi khi xử lý SensorData",
                            observer.getClass().getSimpleName(), e);
                }
            });
        }
    }

    public void messageArrived(String topic, MqttMessage message) {
        try {
            String payload = new String(message.getPayload());

            // Xé nhỏ chuỗi Topic: "USERNAME/feeds/feedKey"
            String[] parts = topic.split("/");
            if (parts.length < 3) {
                log.warn("MqttReceiver: Topic không hợp lệ '{}', bỏ qua message.", topic);
                return;
            }
            String feedKey = parts[parts.length - 1]; // Lấy mảnh đuôi cùng

            log.info("MqttReceiver: Nhận dữ liệu [{}] từ feed [{}]", payload, feedKey);

            // Tra soát ID Thiết bị theo feed key và các alias phổ biến từ Adafruit.
            Optional<Device> deviceOpt = findDeviceByFeedAlias(feedKey);

            if (deviceOpt.isPresent()) {
                Device device = deviceOpt.get();
                Float metricValue = Float.parseFloat(payload);

                // Phát hiện chuyển trạng thái OFFLINE → ONLINE để push WS (chỉ khi thực sự thay đổi)
                boolean wasOffline = device.getConnectionStatus() !=
                        com.yoloFarm.api.enums.ConnectionStatusEnum.ONLINE;

                device.setConnectionStatus(com.yoloFarm.api.enums.ConnectionStatusEnum.ONLINE);
                device.setLastSeen(java.time.LocalDateTime.now());
                deviceRepository.save(device);

                // Push WebSocket event khi thiết bị vừa quay lại ONLINE
                if (wasOffline) {
                    java.util.List<java.util.Map<String, Object>> onlinePayload =
                            java.util.List.of(java.util.Map.<String, Object>of(
                                    "deviceId", device.getId().toString(),
                                    "connectionStatus", "ONLINE"
                            ));
                    messagingTemplate.convertAndSend(
                            "/topic/farm/" + device.getFarm().getId() + "/device-status",
                            (Object) onlinePayload
                    );
                    messagingTemplate.convertAndSend("/topic/admin/stats-changed",
                            (Object) java.util.Map.of("reason", "device_online"));
                    log.info("MqttReceiver: Device [{}] vừa kết nối lại ONLINE.", device.getId());
                }

                String metricType = device.getModel().getMetricType().name();

                SensorData sensorData = new SensorData(
                        device.getFarm().getId(),
                        device.getId(),
                        metricType,
                        metricValue,
                        Instant.now());

                notifyObservers(sensorData);
            } else {
                log.warn("MqttReceiver: FeedKey '{}' chưa được đăng ký cho Nông trại nào trên DB!", feedKey);
            }

        } catch (NumberFormatException nfe) {
            log.warn("MqttReceiver: Dữ liệu Adafruit không phải là số (Ignored message).");
        } catch (Exception e) {
            log.error("MqttReceiver: Lỗi khi xử lý thông điệp Adafruit", e);
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
                    log.info("MqttReceiver: Feed alias '{}' được map sang key '{}'.", rawFeedKey, candidate);
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
            log.debug("MqttReceiver: Pre-warmed cache cho feed key [{}] → device [{}]", feedKey, device.getId());
        }
    }

    /**
     * Xóa cache khi device bị remove hoặc feed key thay đổi.
     */
    public void evictFeedKeyCache(String feedKey) {
        if (feedKey != null) {
            feedKeyCache.remove(feedKey.toLowerCase(Locale.ROOT));
            log.debug("MqttReceiver: Đã evict cache cho feed key [{}]", feedKey);
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
