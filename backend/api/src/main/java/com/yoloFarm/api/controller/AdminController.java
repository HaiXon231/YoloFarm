package com.yoloFarm.api.controller;

import com.yoloFarm.api.dto.response.AdminStatsResponse;
import com.yoloFarm.api.enums.DeviceStatusEnum;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.yoloFarm.api.dto.request.ApproveDeviceRequest;
import com.yoloFarm.api.dto.request.AutomationConfigRequest;
import com.yoloFarm.api.dto.request.DeviceModelRequest;
import com.yoloFarm.api.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import java.util.UUID;
import java.util.Map;
import java.util.List;
import com.yoloFarm.api.dto.response.*;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {
    private final AdminService adminService;

    @GetMapping("/stats")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<AdminStatsResponse> getStats() {
        return ResponseEntity.ok(adminService.getStats());
    }

    @GetMapping("/farmers")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<List<AdminFarmerResponse>> getFarmers() {
        return ResponseEntity.ok(adminService.getFarmers());
    }

    @GetMapping("/farms")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<List<AdminFarmResponse>> getFarms() {
        return ResponseEntity.ok(adminService.getFarms());
    }

    @GetMapping("/devices")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<List<AdminDeviceResponse>> getDevices() {
        return ResponseEntity.ok(adminService.getDevices());
    }

    @PostMapping("/device-models")
    public ResponseEntity<?> createDeviceModel(@Valid @RequestBody DeviceModelRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createDeviceModel(request));
    }

    @DeleteMapping("/devices/{deviceId}")
    public ResponseEntity<?> deleteDevice(@PathVariable("deviceId") UUID deviceId) {
        adminService.deleteDevice(deviceId);
        return ResponseEntity.ok(Map.of("message", "Đã xóa thiết bị khỏi hệ thống"));
    }

    @GetMapping("/automation-config")
    public ResponseEntity<?> getAutomationConfig() {
        return ResponseEntity.ok(adminService.getAutomationConfig());
    }

    @PatchMapping("/automation-config")
    public ResponseEntity<?> updateAutomationConfig(@RequestBody AutomationConfigRequest request) {
        return ResponseEntity.ok(adminService.updateAutomationConfig(request));
    }

    @GetMapping("/devices/requests")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<?> getDeviceRequests(@RequestParam(value = "status", required = false) String status) {
        if (status != null && !status.isBlank()) {
            try {
                DeviceStatusEnum.valueOf(status);
            } catch (IllegalArgumentException ex) {
                throw new IllegalStateException("Giá trị status không hợp lệ: " + status);
            }
        }
        return ResponseEntity.ok(adminService.getDeviceRequests(status));
    }

    @PostMapping("/devices/{deviceId}/approve")
    public ResponseEntity<?> approveDevice(
            @PathVariable("deviceId") UUID deviceId,
            @Valid @RequestBody(required = false) ApproveDeviceRequest request) {
        String adafruitFeedKey = (request == null) ? null : request.getAdafruitFeedKey();
        return ResponseEntity.ok(adminService.approveDevice(deviceId, adafruitFeedKey));
    }

    @PostMapping("/devices/{deviceId}/reject")
    public ResponseEntity<?> rejectDevice(@PathVariable("deviceId") UUID deviceId,
            @RequestBody Map<String, String> request) {
        return ResponseEntity.ok(adminService.rejectDevice(deviceId, request.get("reject_reason")));
    }
}
