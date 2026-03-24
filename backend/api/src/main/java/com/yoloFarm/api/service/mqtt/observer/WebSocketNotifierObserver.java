package com.yoloFarm.api.service.mqtt.observer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
public class WebSocketNotifierObserver implements Observer {
    @Override
    public void update(UUID deviceId, String metricType, Float value) {
        pushToReactFrontend(deviceId, metricType, value);
    }

    private void pushToReactFrontend(UUID deviceId, String metricType, Float value) {
        log.info("Đang bắn WebSocket lên ReactJS cho thiết bị {}: {} = {}", deviceId, metricType, value);
    }
}
