package com.yoloFarm.api.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeviceThresholdRequest {

    private Float minValue;

    private Float maxValue;
}
