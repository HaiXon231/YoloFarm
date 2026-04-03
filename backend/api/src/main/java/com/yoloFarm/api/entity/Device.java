package com.yoloFarm.api.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;
import java.time.LocalDateTime;
import com.yoloFarm.api.enums.DeviceStatusEnum;
import com.yoloFarm.api.enums.ConnectionStatusEnum;
import com.yoloFarm.api.enums.OperatingModeEnum;

@Entity
@Table(name = "devices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Device {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id", nullable = false)
    private DeviceModel model;

    @Column(length = 100, nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeviceStatusEnum status;

    @Column(length = 100, unique = true)
    private String adafruitFeedKey;

    @Enumerated(EnumType.STRING)
    private ConnectionStatusEnum connectionStatus;

    private LocalDateTime lastSeen;

    @Enumerated(EnumType.STRING)
    private OperatingModeEnum operatingMode;

    @Builder.Default
    private Boolean isActive = false;
}
