package com.yoloFarm.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DeviceRenameRequest {
    @NotBlank(message = "Device name cannot be empty")
    private String name;
}
