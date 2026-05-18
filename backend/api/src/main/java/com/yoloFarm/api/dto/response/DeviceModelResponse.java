package com.yoloFarm.api.dto.response;

import lombok.Data;
import java.util.UUID;
import com.yoloFarm.api.enums.DeviceTypeEnum;
import com.yoloFarm.api.enums.MetricTypeEnum;

@Data
public class DeviceModelResponse {
    private UUID id;
    private String modelName;
    private DeviceTypeEnum deviceType;
    private MetricTypeEnum metricType;
    private String manufacturer;
    private String displayUnit;
    private Float minValue;
    private Float maxValue;
    private String modelDescription;
    private String referenceUrl;
}
