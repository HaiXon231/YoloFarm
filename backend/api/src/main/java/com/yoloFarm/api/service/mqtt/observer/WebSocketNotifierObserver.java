package com.yoloFarm.api.service.mqtt.observer;

import com.yoloFarm.api.dto.SensorData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class WebSocketNotifierObserver implements Observer {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void update(SensorData data) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("deviceId", data.deviceId());
            payload.put("metricType", data.metricType());
            payload.put("value", data.value());
            payload.put("timestamp", data.timestamp());

            // farmId đã có sẵn trong SensorData → không cần query DB lại!
            String destination = "/topic/farm/" + data.farmId() + "/telemetry";
            messagingTemplate.convertAndSend(destination, (Object) payload);
            log.info("WebSocketNotifierObserver: Đã đẩy dữ liệu Realtime lên kênh {}", destination);
        } catch (Exception e) {
            log.error("WebSocketNotifierObserver: Lỗi khi đẩy tin nhắn Realtime WebSocket", e);
        }
    }
}
