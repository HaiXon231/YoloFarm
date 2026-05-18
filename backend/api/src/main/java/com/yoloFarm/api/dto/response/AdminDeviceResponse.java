package com.yoloFarm.api.dto.response;

import com.yoloFarm.api.enums.ConnectionStatusEnum;
import com.yoloFarm.api.enums.DeviceStatusEnum;
import com.yoloFarm.api.enums.DeviceTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDeviceResponse {
    private UUID id;
    private String name;
    private String modelName;
    private DeviceTypeEnum deviceType;
    private DeviceStatusEnum status;
    private String farmName;
    private String ownerName;
    private ConnectionStatusEnum connectionStatus;
    private boolean isActive;
}
