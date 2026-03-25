package com.yoloFarm.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.yoloFarm.api.dto.request.FarmCreateRequest;
import com.yoloFarm.api.entity.User;
import com.yoloFarm.api.service.FarmService;
import com.yoloFarm.api.service.DeviceService;
import com.yoloFarm.api.service.RuleService;
import com.yoloFarm.api.service.AiAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.UUID;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/farms")
@RequiredArgsConstructor
public class FarmController {
    private final FarmService farmService;
    private final DeviceService deviceService;
    private final RuleService ruleService;
    private final AiAnalysisService aiAnalysisService;

    @GetMapping
    public ResponseEntity<?> getFarms() {
        User currentUser = (User) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return ResponseEntity.ok(farmService.getFarmsByUserId(currentUser.getId()));
    }

    @PostMapping
    public ResponseEntity<?> createFarm(@RequestBody FarmCreateRequest request) {
        User currentUser = (User) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(farmService.createFarm(request, currentUser.getId()));
    }

    @GetMapping("/{farmId}")
    public ResponseEntity<?> getFarmDetails(@PathVariable("farmId") UUID farmId) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(Map.of("message", "Chức năng đang phát triển"));
    }

    @PutMapping("/{farmId}")
    public ResponseEntity<?> updateFarm(@PathVariable("farmId") UUID farmId, @RequestBody FarmCreateRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(Map.of("message", "Chức năng đang phát triển"));
    }

    @DeleteMapping("/{farmId}")
    public ResponseEntity<?> deleteFarm(@PathVariable("farmId") UUID farmId) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(Map.of("message", "Chức năng đang phát triển"));
    }

    @GetMapping("/{farmId}/devices")
    public ResponseEntity<?> getFarmDevices(@PathVariable("farmId") UUID farmId) {
        return ResponseEntity.ok(deviceService.getDevicesByFarmId(farmId));
    }

    @GetMapping("/{farmId}/rules")
    public ResponseEntity<?> getFarmRules(@PathVariable("farmId") UUID farmId) {
        return ResponseEntity.ok(ruleService.getRulesByFarmId(farmId));
    }

    @PostMapping("/{farmId}/ai-analysis")
    public ResponseEntity<?> analyzeAi(@PathVariable("farmId") UUID farmId, 
                                       @RequestParam("file") MultipartFile file, 
                                       @RequestParam("analysis_type") String analysisType) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(Map.of("message", "Chức năng đang phát triển"));
    }

    @GetMapping("/{farmId}/ai-logs")
    public ResponseEntity<?> getAiLogs(@PathVariable("farmId") UUID farmId) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(Map.of("message", "Chức năng đang phát triển"));
    }
}
