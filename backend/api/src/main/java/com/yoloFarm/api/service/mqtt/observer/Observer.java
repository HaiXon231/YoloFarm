package com.yoloFarm.api.service.mqtt.observer;

import java.util.UUID;

public interface Observer {
    void update(UUID deviceId, String metricType, Float value);
}
