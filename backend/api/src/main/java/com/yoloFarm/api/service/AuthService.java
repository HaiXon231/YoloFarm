package com.yoloFarm.api.service;

import com.yoloFarm.api.dto.request.LoginRequest;
import com.yoloFarm.api.dto.request.RegisterRequest;
import com.yoloFarm.api.dto.response.LoginResponse;
import com.yoloFarm.api.dto.response.UserProfile;

public interface AuthService {
    UserProfile register(RegisterRequest request);
    LoginResponse login(LoginRequest request);
}
