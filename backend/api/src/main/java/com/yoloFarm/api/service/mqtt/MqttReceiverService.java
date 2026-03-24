package com.yoloFarm.api.service.mqtt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yoloFarm.api.service.mqtt.observer.Observer;
import com.yoloFarm.api.service.mqtt.observer.Subject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class MqttReceiverService implements Subject {

    private final List<Observer> injectedObservers;
    private final List<Observer> observers = new ArrayList<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        if (injectedObservers != null) {
            injectedObservers.forEach(this::attach);
            log.info("Tự động attach {} observers vào MqttReceiverService", injectedObservers.size());
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
    public void notifyObservers(UUID deviceId, String metricType, Float value) {
        for (Observer observer : observers) {
            observer.update(deviceId, metricType, value);
        }
    }

    public void simulateMessageReceived(String topic, String payload) {
        log.info("Received MQTT message from topic: {}, payload: {}", topic, payload);
        try {
            JsonNode node = objectMapper.readTree(payload);
            
            UUID deviceId = UUID.fromString(node.get("deviceId").asText());
            String metricType = node.get("metricType").asText();
            Float value = (float) node.get("value").asDouble();

            log.info("Bóc tách thành công: deviceId={}, metricType={}, value={}", deviceId, metricType, value);
            notifyObservers(deviceId, metricType, value);
        } catch (Exception e) {
            log.error("Lỗi khi bóc tách JSON MQTT payload", e);
        }
    }
}
