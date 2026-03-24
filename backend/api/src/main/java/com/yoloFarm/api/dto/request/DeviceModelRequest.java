package com.yoloFarm.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import com.yoloFarm.api.enums.DeviceTypeEnum;
import com.yoloFarm.api.enums.MetricTypeEnum;

@Data
public class DeviceModelRequest {
    @NotBlank(message = "Model name cannot be empty")
    private String modelName;

    @NotNull(message = "Device type cannot be null")
    private DeviceTypeEnum deviceType;

    @NotNull(message = "Metric type cannot be null")
    private MetricTypeEnum metricType;

    private String manufacturer;
}
