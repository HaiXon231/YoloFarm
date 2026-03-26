package com.yoloFarm.api.controller;

import com.yoloFarm.api.entity.User;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.yoloFarm.api.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<?> getNotifications(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return ResponseEntity.ok(notificationService.getNotifications(currentUser.getId(), page, size));
    }

    @PutMapping("/{notificationId}/read")
    public ResponseEntity<?> markAsRead(@AuthenticationPrincipal User currentUser, @PathVariable("notificationId") UUID notificationId) {
        notificationService.markAsRead(notificationId, currentUser.getId());
        return ResponseEntity.ok(java.util.Map.of("message", "Đã đánh dấu đọc."));
    }
}
