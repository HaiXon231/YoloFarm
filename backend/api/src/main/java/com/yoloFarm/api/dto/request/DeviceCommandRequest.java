package com.yoloFarm.api.dto.request;

import com.yoloFarm.api.enums.ActionCommandEnum;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DeviceCommandRequest {

    @NotNull(message = "Trường command không được để trống")
    private ActionCommandEnum command;
}
