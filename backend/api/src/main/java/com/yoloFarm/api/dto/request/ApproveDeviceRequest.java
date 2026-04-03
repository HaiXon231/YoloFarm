package com.yoloFarm.api.dto.request;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ApproveDeviceRequest {

    @Pattern(regexp = "^[a-z0-9-]{1,64}$", message = "Feed key phải đúng chuẩn Adafruit: chỉ gồm a-z, 0-9 và dấu '-' (1-64 ký tự)")
    private String adafruitFeedKey;
}