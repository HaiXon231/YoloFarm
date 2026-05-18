package com.yoloFarm.api.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AutomationConfigRequest {

    private Integer maxAutoOnMinutes;

    private Integer commandCooldownSeconds;
}
