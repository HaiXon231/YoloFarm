package com.yoloFarm.api.controller;

import com.yoloFarm.api.enums.DeviceStatusEnum;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.yoloFarm.api.dto.request.DeviceModelRequest;
import com.yoloFarm.api.service.DeviceModelService;
import com.yoloFarm.api.service.DeviceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import java.util.UUID;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {
    private final DeviceModelService deviceModelService;
    private final DeviceService deviceService;

    @PostMapping("/device-models")
    public ResponseEntity<?> createDeviceModel(@Valid @RequestBody DeviceModelRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(deviceModelService.createDeviceModel(request));
    }

    @DeleteMapping("/devices/{deviceId}")
    public ResponseEntity<?> deleteDevice(@PathVariable("deviceId") UUID deviceId) {
        deviceService.deleteDevice(deviceId);
        return ResponseEntity.ok(Map.of("message", "Đã xóa thiết bị khỏi hệ thống"));
    }

    @GetMapping("/devices/requests")
    public ResponseEntity<?> getDeviceRequests(@RequestParam(value = "status", required = false) String status) {
        if (status != null && !status.isBlank()) {
            try {
                DeviceStatusEnum.valueOf(status);
            } catch (IllegalArgumentException ex) {
                throw new IllegalStateException("Giá trị status không hợp lệ: " + status);
            }
        }
        return ResponseEntity.ok(deviceService.getDeviceRequests(status));
    }

    @PostMapping("/devices/{deviceId}/approve")
    public ResponseEntity<?> approveDevice(
            @PathVariable("deviceId") UUID deviceId,
            @RequestBody(required = false) Map<String, String> request) {
        String adafruitFeedKey = (request == null) ? null : request.get("adafruit_feed_key");
        return ResponseEntity.ok(deviceService.approveDevice(deviceId, adafruitFeedKey));
    }

    @PostMapping("/devices/{deviceId}/reject")
    public ResponseEntity<?> rejectDevice(@PathVariable("deviceId") UUID deviceId,
            @RequestBody Map<String, String> request) {
        deviceService.rejectDevice(deviceId, request.get("reject_reason"));
        return ResponseEntity.ok(Map.of("message", "Đã từ chối yêu cầu và gửi thông báo cho Nông dân."));
    }
}
