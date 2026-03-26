package com.yoloFarm.api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "telemetry_data")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TelemetryData {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "device_id", nullable = false)
    private UUID deviceId;

    @Column(name = "metric_type", nullable = false, length = 50)
    private String metricType;

    @Column(name = "value", nullable = false)
    private Float value;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
