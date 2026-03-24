package com.yoloFarm.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.yoloFarm.api.dto.request.FarmCreateRequest;
import com.yoloFarm.api.service.FarmService;
import com.yoloFarm.api.service.DeviceService;
import com.yoloFarm.api.service.RuleService;
import com.yoloFarm.api.service.AiAnalysisService;
import lombok.RequiredArgsConstructor;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api/v1/farms")
@RequiredArgsConstructor
public class FarmController {
    private final FarmService farmService;
    private final DeviceService deviceService;
    private final RuleService ruleService;
    private final AiAnalysisService aiAnalysisService;

    @GetMapping
    public ResponseEntity<?> getFarms(@RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(farmService.getFarmsByUserId(userId));
    }

    @PostMapping
    public ResponseEntity<?> createFarm(@RequestHeader("X-User-Id") UUID userId, @RequestBody FarmCreateRequest request) {
        return ResponseEntity.ok(farmService.createFarm(request, userId));
    }

    @GetMapping("/{farmId}")
    public ResponseEntity<?> getFarmDetails(@PathVariable("farmId") UUID farmId) {
        return ResponseEntity.ok(null);
    }

    @PutMapping("/{farmId}")
    public ResponseEntity<?> updateFarm(@PathVariable("farmId") UUID farmId, @RequestBody FarmCreateRequest request) {
        return ResponseEntity.ok(null);
    }

    @DeleteMapping("/{farmId}")
    public ResponseEntity<?> deleteFarm(@PathVariable("farmId") UUID farmId) {
        return ResponseEntity.ok(null);
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
        return ResponseEntity.ok(null);
    }

    @GetMapping("/{farmId}/ai-logs")
    public ResponseEntity<?> getAiLogs(@PathVariable("farmId") UUID farmId) {
        return ResponseEntity.ok(null);
    }
}
