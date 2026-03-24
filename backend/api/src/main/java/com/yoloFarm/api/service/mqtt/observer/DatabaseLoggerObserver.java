package com.yoloFarm.api.service.mqtt.observer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
public class DatabaseLoggerObserver implements Observer {
    @Override
    public void update(UUID deviceId, String metricType, Float value) {
        saveToTimeSeriesDB(deviceId, metricType, value);
    }

    private void saveToTimeSeriesDB(UUID deviceId, String metricType, Float value) {
        log.info("Đang lưu dữ liệu {} ({}) của thiết bị {} vào Database Time-Series", metricType, value, deviceId);
    }
}
