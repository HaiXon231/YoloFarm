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
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
@RequiredArgsConstructor
public class MqttReceiverService implements Subject {

    private final List<Observer> injectedObservers;
    private final List<Observer> observers = new ArrayList<>();
    
    private final IMqttClient mqttClient;
    private final DeviceRepository deviceRepository;
    private final AtomicBoolean subscribed = new AtomicBoolean(false);

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
            observer.update(data);
        }
    }

    public void messageArrived(String topic, MqttMessage message) {
        try {
            String payload = new String(message.getPayload());
            
            // Xé nhỏ chuỗi Topic: "USERNAME/feeds/feedKey"
            String[] parts = topic.split("/");
            String feedKey = parts[parts.length - 1]; // Lấy mảnh đuôi cùng

            log.info("MqttReceiver: Nhận dữ liệu [{}] từ feed [{}]", payload, feedKey);

            // Tra soát ID Thiết bị theo đoạn mã feed trên Adafruit
            // Tra soát ID Thiết bị theo đoạn mã feed trên Adafruit (JOIN FETCH để tránh LazyInit)
            Optional<Device> deviceOpt = deviceRepository.findByAdafruitFeedKeyWithModelAndFarm(feedKey);
            
            if (deviceOpt.isPresent()) {
                Device device = deviceOpt.get();
                // Ép chuỗi raw data Adafruit sang con số thập phân
                Float metricValue = Float.parseFloat(payload);                
                // Cập nhật trạng thái kết nối và lastSeen của thiết bị
                device.setConnectionStatus(com.yoloFarm.api.enums.ConnectionStatusEnum.ONLINE);
                device.setLastSeen(java.time.LocalDateTime.now());
                deviceRepository.save(device);

                // Dùng getMetricType() thay vì getDeviceType() để lấy đúng loại metric
                String metricType = device.getModel().getMetricType().name();

                // Đóng gói toàn bộ context vào SensorData
                SensorData sensorData = new SensorData(
                        device.getFarm().getId(),
                        device.getId(),
                        metricType,
                        metricValue,
                        Instant.now()
                );

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
}
