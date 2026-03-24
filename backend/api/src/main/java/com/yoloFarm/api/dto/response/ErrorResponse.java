package com.yoloFarm.api.dto.response;

import lombok.Data;

@Data
public class ErrorResponse {
    private Integer code;
    private String message;
    private String details;
}
