package com.yoloFarm.api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.yoloFarm.api.dto.request.DeviceModelRequest;
import com.yoloFarm.api.service.DeviceModelService;
import com.yoloFarm.api.service.DeviceService;
import lombok.RequiredArgsConstructor;
import java.util.UUID;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {
    private final DeviceModelService deviceModelService;
    private final DeviceService deviceService;

    @PostMapping("/device-models")
    public ResponseEntity<?> createDeviceModel(@RequestBody DeviceModelRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(Map.of("message", "Chức năng đang phát triển"));
    }

    @DeleteMapping("/devices/{deviceId}")
    public ResponseEntity<?> deleteDevice(@PathVariable("deviceId") UUID deviceId) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(Map.of("message", "Chức năng đang phát triển"));
    }

    @GetMapping("/devices/requests")
    public ResponseEntity<?> getDeviceRequests(@RequestParam(value = "status", required = false) String status) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(Map.of("message", "Chức năng đang phát triển"));
    }

    @PostMapping("/devices/{deviceId}/approve")
    public ResponseEntity<?> approveDevice(@PathVariable("deviceId") UUID deviceId, @RequestBody Map<String, String> request) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(Map.of("message", "Chức năng đang phát triển"));
    }

    @PostMapping("/devices/{deviceId}/reject")
    public ResponseEntity<?> rejectDevice(@PathVariable("deviceId") UUID deviceId, @RequestBody Map<String, String> request) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(Map.of("message", "Chức năng đang phát triển"));
    }
}
