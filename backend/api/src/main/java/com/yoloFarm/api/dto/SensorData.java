package com.yoloFarm.api.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO chứa toàn bộ context dữ liệu cảm biến từ MQTT.
 * Dùng record (Java 16+) vì DTO là immutable by design.
 */
public record SensorData(
    UUID farmId,
    UUID deviceId,
    String metricType,
    Float value,
    Instant timestamp
) {}
