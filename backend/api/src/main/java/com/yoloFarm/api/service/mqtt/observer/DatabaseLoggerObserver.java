package com.yoloFarm.api.service.mqtt.observer;

import com.yoloFarm.api.dto.SensorData;
import com.yoloFarm.api.entity.TelemetryData;
import com.yoloFarm.api.repository.TelemetryDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Slf4j
@RequiredArgsConstructor
public class DatabaseLoggerObserver implements Observer {

    private final TelemetryDataRepository telemetryDataRepository;

    @Override
    public void update(SensorData data) {
        try {
            TelemetryData telemetry = TelemetryData.builder()
                    .deviceId(data.deviceId())
                    .metricType(data.metricType())
                    .value(data.value())
                    .createdAt(LocalDateTime.now())
                    .build();
            telemetryDataRepository.save(telemetry);
            log.info("DatabaseLoggerObserver: Đã lưu TelemetryData thành công cho Device {}", data.deviceId());
        } catch (Exception e) {
            log.error("DatabaseLoggerObserver: Lỗi khi lưu TelemetryData vào DB", e);
        }
    }
}
