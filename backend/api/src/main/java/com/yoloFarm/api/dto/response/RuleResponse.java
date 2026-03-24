package com.yoloFarm.api.dto.response;

import lombok.Data;
import java.util.UUID;
import com.yoloFarm.api.enums.RuleTypeEnum;
import com.yoloFarm.api.enums.ActionCommandEnum;

@Data
public class RuleResponse {
    private UUID id;
    private UUID farmId;
    private String ruleName;
    private RuleTypeEnum ruleType;
    private UUID triggerDeviceId;
    private String operator;
    private Float thresholdValue;
    private String cronExpression;
    private UUID actionDeviceId;
    private ActionCommandEnum actionCommand;
    private Boolean isActive;
}
