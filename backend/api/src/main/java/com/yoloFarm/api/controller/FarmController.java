package com.yoloFarm.api.controller;

import com.yoloFarm.api.entity.User;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.yoloFarm.api.dto.request.FarmCreateRequest;
import com.yoloFarm.api.service.FarmService;
import com.yoloFarm.api.service.DeviceService;
import com.yoloFarm.api.service.RuleService;
import com.yoloFarm.api.service.AiAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.HttpStatus;
import jakarta.validation.Valid;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/farms")
@RequiredArgsConstructor
public class FarmController {
    private final FarmService farmService;
    private final DeviceService deviceService;
    private final RuleService ruleService;
    private final AiAnalysisService aiAnalysisService;

    @GetMapping
    public ResponseEntity<?> getFarms(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(farmService.getFarmsByUserId(currentUser.getId()));
    }

    @PostMapping
    public ResponseEntity<?> createFarm(@AuthenticationPrincipal User currentUser,
            @Valid @RequestBody FarmCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(farmService.createFarm(request, currentUser.getId()));
    }

    @GetMapping("/{farmId}")
    public ResponseEntity<?> getFarmDetails(@AuthenticationPrincipal User currentUser,
            @PathVariable("farmId") UUID farmId) {
        return ResponseEntity.ok(farmService.getFarmById(farmId, currentUser.getId()));
    }

    @PutMapping("/{farmId}")
    public ResponseEntity<?> updateFarm(
            @AuthenticationPrincipal User currentUser,
            @PathVariable("farmId") UUID farmId,
            @Valid @RequestBody FarmCreateRequest request) {
        return ResponseEntity.ok(farmService.updateFarm(farmId, currentUser.getId(), request));
    }

    @DeleteMapping("/{farmId}")
    public ResponseEntity<?> deleteFarm(@AuthenticationPrincipal User currentUser,
            @PathVariable("farmId") UUID farmId) {
        farmService.deleteFarm(farmId, currentUser.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{farmId}/devices")
    public ResponseEntity<?> getFarmDevices(@AuthenticationPrincipal User currentUser,
            @PathVariable("farmId") UUID farmId) {
        return ResponseEntity.ok(deviceService.getDevicesByFarmId(farmId, currentUser.getId()));
    }

    @GetMapping("/{farmId}/rules")
    public ResponseEntity<?> getFarmRules(@AuthenticationPrincipal User currentUser,
            @PathVariable("farmId") UUID farmId) {
        return ResponseEntity.ok(ruleService.getRulesByFarmId(farmId, currentUser.getId()));
    }

    @PostMapping("/{farmId}/ai-analysis")
    public ResponseEntity<?> analyzeAi(@AuthenticationPrincipal User currentUser,
            @PathVariable("farmId") UUID farmId,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @RequestParam("analysis_type") String analysisType) {
        return ResponseEntity.ok(aiAnalysisService.analyzeImage(currentUser.getId(), farmId, file, analysisType));
    }

    @GetMapping("/{farmId}/ai-logs")
    public ResponseEntity<?> getAiLogs(@AuthenticationPrincipal User currentUser, @PathVariable("farmId") UUID farmId) {
        return ResponseEntity.ok(aiAnalysisService.getLogs(currentUser.getId(), farmId));
    }
}
