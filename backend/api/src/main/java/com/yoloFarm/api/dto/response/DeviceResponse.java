package com.yoloFarm.api.dto.response;

import lombok.Data;
import java.util.UUID;
import java.time.LocalDateTime;
import com.yoloFarm.api.enums.DeviceStatusEnum;
import com.yoloFarm.api.enums.ConnectionStatusEnum;
import com.yoloFarm.api.enums.OperatingModeEnum;

@Data
public class DeviceResponse {
    private UUID id;
    private UUID farmId;
    private UUID modelId;
    private String name;
    private DeviceStatusEnum status;
    private String adafruitFeedKey;
    private ConnectionStatusEnum connectionStatus;
    private LocalDateTime lastSeen;
    private OperatingModeEnum operatingMode;
    private Boolean isActive;
}
