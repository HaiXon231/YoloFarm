package com.yoloFarm.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.yoloFarm.api.dto.request.RuleCreateRequest;
import com.yoloFarm.api.entity.User;
import com.yoloFarm.api.service.RuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import java.util.UUID;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/rules")
@RequiredArgsConstructor
public class RuleController {
    private final RuleService ruleService;

    @PostMapping
    public ResponseEntity<?> createRule(@AuthenticationPrincipal User currentUser, @Valid @RequestBody RuleCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ruleService.createRule(request, currentUser.getId()));
    }

    @PatchMapping("/{ruleId}/toggle")
    public ResponseEntity<?> toggleRule(
            @AuthenticationPrincipal User currentUser,
            @PathVariable("ruleId") UUID ruleId,
            @RequestBody Map<String, Boolean> request
    ) {
        Boolean isActive = request.get("is_active");
        if (isActive == null) {
            isActive = request.getOrDefault("isActive", true);
        }
        ruleService.toggleRule(ruleId, isActive, currentUser.getId());
        return ResponseEntity.ok(Map.of("message", isActive ? "Đã bật Rule thành công." : "Đã tắt Rule thành công."));
    }

    @DeleteMapping("/{ruleId}")
    public ResponseEntity<?> deleteRule(@AuthenticationPrincipal User currentUser, @PathVariable("ruleId") UUID ruleId) {
        ruleService.deleteRule(ruleId, currentUser.getId());
        return ResponseEntity.ok(Map.of("message", "Đã xóa Rule thành công."));
    }

    @PutMapping("/{ruleId}")
    public ResponseEntity<?> updateRule(
            @AuthenticationPrincipal User currentUser,
            @PathVariable("ruleId") UUID ruleId,
            @Valid @RequestBody RuleCreateRequest request
    ) {
        return ResponseEntity.ok(ruleService.updateRule(ruleId, request, currentUser.getId()));
    }
}
