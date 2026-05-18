package com.yoloFarm.api.repository.projection;

import com.yoloFarm.api.enums.ConnectionStatusEnum;
import com.yoloFarm.api.enums.DeviceStatusEnum;
import com.yoloFarm.api.enums.DeviceTypeEnum;

import java.util.UUID;

public interface AdminDeviceProjection {
    UUID getId();

    String getName();

    String getModelName();

    DeviceTypeEnum getDeviceType();

    DeviceStatusEnum getStatus();

    String getFarmName();

    String getOwnerName();

    ConnectionStatusEnum getConnectionStatus();

    Boolean getIsActive();
}
