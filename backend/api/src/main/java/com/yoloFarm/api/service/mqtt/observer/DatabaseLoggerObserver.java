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
            // Dùng timestamp từ SensorData và chuan hoa theo mui gio Viet Nam (UTC+7).
            java.time.LocalDateTime receivedAt = java.time.LocalDateTime
                    .ofInstant(data.timestamp(), java.time.ZoneId.of("Asia/Ho_Chi_Minh"));

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
