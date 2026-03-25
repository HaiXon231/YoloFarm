package com.yoloFarm.api.service.mqtt.observer;

import com.yoloFarm.api.dto.SensorData;

public interface Subject {
    void attach(Observer o);
    void detach(Observer o);
    void notifyObservers(SensorData data);
}
