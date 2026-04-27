package com.yoloFarm.api.service.mqtt.observer;

import com.yoloFarm.api.dto.SensorData;
import com.yoloFarm.api.entity.TelemetryData;
import com.yoloFarm.api.repository.TelemetryDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class DatabaseLoggerObserver implements Observer {

    private final TelemetryDataRepository telemetryDataRepository;

    @Override
    public void update(SensorData data) {
        try {
            // BUG-10: Dùng timestamp từ SensorData (thời điểm thực nhận MQTT message)
            // thay vì LocalDateTime.now() với hardcoded timezone. Nhất quán và portable.
            java.time.LocalDateTime receivedAt = java.time.LocalDateTime
                    .ofInstant(data.timestamp(), java.time.ZoneOffset.UTC);

            TelemetryData telemetry = TelemetryData.builder()
                    .deviceId(data.deviceId())
                    .metricType(data.metricType())
                    .value(data.value())
                    .createdAt(receivedAt)
                    .build();
            telemetryDataRepository.save(telemetry);
            log.info("DatabaseLoggerObserver: Successfully saved TelemetryData for Device {}", data.deviceId());
        } catch (Exception e) {
            log.error("DatabaseLoggerObserver: Error saving TelemetryData to DB", e);
        }
    }
}
