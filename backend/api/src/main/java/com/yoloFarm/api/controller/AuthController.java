package com.yoloFarm.api.controller;

import com.yoloFarm.api.dto.request.LoginRequest;
import com.yoloFarm.api.dto.request.RegisterRequest;
import com.yoloFarm.api.dto.response.LoginResponse;
import com.yoloFarm.api.dto.response.UserProfile;
import com.yoloFarm.api.entity.User;
import com.yoloFarm.api.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<UserProfile> register(@Valid @RequestBody RegisterRequest request) {
        UserProfile profile = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(profile);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfile> getCurrentUser(@AuthenticationPrincipal User currentUser) {
        UserProfile profile = new UserProfile();
        profile.setId(currentUser.getId());
        profile.setUsername(currentUser.getUsername());
        profile.setEmail(currentUser.getEmail());
        profile.setRole(currentUser.getRole().name());
        profile.setCreatedAt(currentUser.getCreatedAt());
        return ResponseEntity.ok(profile);
    }
}
