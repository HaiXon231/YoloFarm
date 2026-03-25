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
import org.springframework.security.core.context.SecurityContextHolder;
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
    public ResponseEntity<UserProfile> getCurrentUser() {
        User user = (User) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        UserProfile profile = new UserProfile();
        profile.setId(user.getId());
        profile.setUsername(user.getUsername());
        profile.setEmail(user.getEmail());
        profile.setRole(user.getRole().name());
        profile.setCreatedAt(user.getCreatedAt());
        return ResponseEntity.ok(profile);
    }
}
