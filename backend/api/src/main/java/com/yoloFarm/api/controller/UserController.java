package com.yoloFarm.api.controller;

import com.yoloFarm.api.dto.request.UpdateProfileRequest;
import com.yoloFarm.api.dto.response.UserProfile;
import com.yoloFarm.api.entity.User;
import com.yoloFarm.api.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public ResponseEntity<UserProfile> getProfile(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(userService.getCurrentProfile(user.getEmail()));
    }

    @PatchMapping("/profile")
    public ResponseEntity<UserProfile> updateProfile(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return ResponseEntity.ok(userService.updateProfile(user.getEmail(), request));
    }
}
