package com.yoloFarm.api.dto.response;

import lombok.Data;

@Data
public class LoginResponse {
    private String accessToken;
    private String tokenType;
    private Integer expiresIn;
    private String role;
}
