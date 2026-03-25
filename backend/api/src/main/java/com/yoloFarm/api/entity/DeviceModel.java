package com.yoloFarm.api.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;
import java.util.List;
import com.yoloFarm.api.enums.DeviceTypeEnum;
import com.yoloFarm.api.enums.MetricTypeEnum;

@Entity
@Table(name = "models")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceModel {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(length = 100, nullable = false)
    private String modelName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeviceTypeEnum deviceType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MetricTypeEnum metricType;

    @Column(length = 100)
    private String manufacturer;

    @OneToMany(mappedBy = "model", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Device> devices;
}
