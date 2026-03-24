package com.yoloFarm.api.service.mqtt.observer;

import java.util.UUID;

public interface Subject {
    void attach(Observer o);
    void detach(Observer o);
    void notifyObservers(UUID deviceId, String metricType, Float value);
}
