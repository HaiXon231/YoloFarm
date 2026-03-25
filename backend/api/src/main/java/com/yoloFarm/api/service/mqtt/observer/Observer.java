package com.yoloFarm.api.service.mqtt.observer;

import com.yoloFarm.api.dto.SensorData;

public interface Observer {
    void update(SensorData data);
}
