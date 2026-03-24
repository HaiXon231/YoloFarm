package com.yoloFarm.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FarmCreateRequest {
    @NotBlank(message = "Farm name cannot be empty")
    private String name;

    private String location;
}
