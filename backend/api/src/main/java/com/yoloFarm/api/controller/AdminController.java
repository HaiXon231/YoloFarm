package com.yoloFarm.api.controller;

import com.yoloFarm.api.dto.response.AdminStatsResponse;
import com.yoloFarm.api.enums.DeviceStatusEnum;
import com.yoloFarm.api.enums.RoleEnum;
import com.yoloFarm.api.repository.FarmRepository;
import com.yoloFarm.api.repository.UserRepository;
import com.yoloFarm.api.repository.DeviceRepository;
import org.eclipse.paho.client.mqttv3.IMqttClient;
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
import java.util.List;
import java.util.stream.Collectors;
import com.yoloFarm.api.dto.response.*;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {
    private final DeviceModelService deviceModelService;
    private final DeviceService deviceService;
    private final UserRepository userRepository;
    private final FarmRepository farmRepository;
    private final DeviceRepository deviceRepository;
    private final IMqttClient mqttClient;

    @GetMapping("/stats")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<AdminStatsResponse> getStats() {
        AdminStatsResponse stats = AdminStatsResponse.builder()
                .totalFarmers(userRepository.countByRole(RoleEnum.FARMER))
                .totalFarms(farmRepository.count())
                .totalDevices(deviceRepository.count())
                .pendingRequests(deviceRepository.countByStatus(DeviceStatusEnum.PENDING))
                .activeDevices(deviceRepository.countByStatus(DeviceStatusEnum.ACTIVE))
                .apiStatus(true) // If this method is called, API is up
                .mqttStatus(mqttClient.isConnected())
                .dbStatus(true) // If counts above succeeded, DB is up
                .build();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/farmers")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<List<AdminFarmerResponse>> getFarmers() {
        List<AdminFarmerResponse> farmers = userRepository.findByRole(RoleEnum.FARMER).stream()
                .map(u -> AdminFarmerResponse.builder()
                        .id(u.getId())
                        .username(u.getUsername())
                        .email(u.getEmail())
                        .createdAt(u.getCreatedAt())
                        .farmCount(u.getFarms() != null ? u.getFarms().size() : 0)
                        .build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(farmers);
    }

    @GetMapping("/farms")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<List<AdminFarmResponse>> getFarms() {
        List<AdminFarmResponse> farms = farmRepository.findAll().stream()
                .map(f -> AdminFarmResponse.builder()
                        .id(f.getId())
                        .name(f.getName())
                        .location(f.getLocation())
                        .ownerName(f.getOwner().getUsername())
                        .ownerEmail(f.getOwner().getEmail())
                        .createdAt(f.getCreatedAt())
                        .deviceCount(f.getDevices() != null ? f.getDevices().size() : 0)
                        .build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(farms);
    }

    @GetMapping("/devices")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<List<AdminDeviceResponse>> getDevices() {
        List<AdminDeviceResponse> devices = deviceRepository.findAll().stream()
                .map(d -> AdminDeviceResponse.builder()
                        .id(d.getId())
                        .name(d.getName())
                        .modelName(d.getModel().getModelName())
                        .status(d.getStatus())
                        .farmName(d.getFarm().getName())
                        .ownerName(d.getFarm().getOwner().getUsername())
                        .connectionStatus(d.getConnectionStatus())
                        .isActive(Boolean.TRUE.equals(d.getIsActive()))
                        .build())
                .collect(Collectors.<AdminDeviceResponse>toList());
        return ResponseEntity.ok(devices);
    }

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
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
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
