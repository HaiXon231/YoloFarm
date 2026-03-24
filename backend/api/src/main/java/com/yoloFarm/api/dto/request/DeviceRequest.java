package com.yoloFarm.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.UUID;

@Data
public class DeviceRequest {
    @NotNull(message = "Farm ID cannot be null")
    private UUID farmId;

    @NotNull(message = "Model ID cannot be null")
    private UUID modelId;

    @NotBlank(message = "Device name cannot be empty")
    private String name;
}
