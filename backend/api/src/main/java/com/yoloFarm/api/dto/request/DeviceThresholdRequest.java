package com.yoloFarm.api.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeviceThresholdRequest {

    @JsonAlias("minValue")
    private Float minValue;

    @JsonAlias("maxValue")
    private Float maxValue;
}
