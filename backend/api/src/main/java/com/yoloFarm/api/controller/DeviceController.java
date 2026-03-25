package com.yoloFarm.api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.yoloFarm.api.dto.request.DeviceRequest;
import com.yoloFarm.api.service.DeviceService;
import com.yoloFarm.api.service.TelemetryService;
import com.yoloFarm.api.service.ControlService;
import com.yoloFarm.api.service.strategy.IrrigationContext;
import com.yoloFarm.api.service.strategy.ManualStrategy;
import lombok.RequiredArgsConstructor;
import java.util.UUID;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
public class DeviceController {
    private final DeviceService deviceService;
    private final TelemetryService telemetryService;
    private final ControlService controlService;
    private final IrrigationContext irrigationContext;
    private final ManualStrategy manualStrategy;

    @PatchMapping("/{deviceId}")
    public ResponseEntity<?> updateDeviceName(@PathVariable("deviceId") UUID deviceId, @RequestBody Map<String, String> request) {
        String newName = request.get("name");
        String statusStr = request.get("status");
        com.yoloFarm.api.enums.DeviceStatusEnum statusEnum = null;
        if (statusStr != null) {
            statusEnum = com.yoloFarm.api.enums.DeviceStatusEnum.valueOf(statusStr);
        }
        return ResponseEntity.ok(deviceService.updateDevice(deviceId, newName, statusEnum));
    }

    @PostMapping("/{deviceId}/remove-requests")
    public ResponseEntity<?> requestDeviceRemoval(@PathVariable("deviceId") UUID deviceId) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(Map.of("message", "Chức năng đang phát triển"));
    }

    @PostMapping("/requests")
    public ResponseEntity<?> requestNewDevice(@RequestBody DeviceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(deviceService.addDevice(request));
    }

    @GetMapping("/{deviceId}/telemetry")
    public ResponseEntity<?> getTelemetry(
            @PathVariable("deviceId") UUID deviceId,
            @RequestParam("start_time") String startTime,
            @RequestParam("end_time") String endTime,
            @RequestParam(value = "aggregate", required = false) String aggregate) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(Map.of("message", "Chức năng đang phát triển"));
    }

    @PostMapping("/{deviceId}/command")
    public ResponseEntity<?> sendCommand(@PathVariable("deviceId") UUID deviceId, @RequestBody Map<String, String> request) {
        boolean success = irrigationContext.executeControl(manualStrategy, null, deviceId, request.get("command"));
        return ResponseEntity.ok(Map.of("message", "Lệnh đã được gửi thành công", "success", success));
    }

    @PatchMapping("/{deviceId}/mode")
    public ResponseEntity<?> changeMode(@PathVariable("deviceId") UUID deviceId, @RequestBody Map<String, String> request) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(Map.of("message", "Chức năng đang phát triển"));
    }
}
