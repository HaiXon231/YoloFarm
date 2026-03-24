package com.yoloFarm.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.UUID;
import com.yoloFarm.api.enums.RuleTypeEnum;
import com.yoloFarm.api.enums.ActionCommandEnum;

@Data
public class RuleCreateRequest {
    @NotNull(message = "Farm ID cannot be null")
    private UUID farmId;

    @NotBlank(message = "Rule name cannot be empty")
    private String ruleName;

    @NotNull(message = "Rule type cannot be null")
    private RuleTypeEnum ruleType;

    private UUID triggerDeviceId;

    private String operator;

    private Float thresholdValue;

    private String cronExpression;

    @NotNull(message = "Action device ID cannot be null")
    private UUID actionDeviceId;

    @NotNull(message = "Action command cannot be null")
    private ActionCommandEnum actionCommand;
}
