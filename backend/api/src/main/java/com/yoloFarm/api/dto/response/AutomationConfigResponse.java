package com.yoloFarm.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutomationConfigResponse {

    private Integer maxAutoOnMinutes;

    private Integer commandCooldownSeconds;
}
