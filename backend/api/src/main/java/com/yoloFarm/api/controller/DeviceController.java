package com.yoloFarm.api.controller;

import com.yoloFarm.api.dto.request.DeviceRenameRequest;
import com.yoloFarm.api.enums.OperatingModeEnum;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.yoloFarm.api.dto.request.DeviceRequest;
import com.yoloFarm.api.entity.User;
import com.yoloFarm.api.service.DeviceService;
import com.yoloFarm.api.service.TelemetryService;
import com.yoloFarm.api.service.ControlService;
import com.yoloFarm.api.service.strategy.IrrigationContext;
import com.yoloFarm.api.service.strategy.ManualStrategy;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.HttpStatus;
import java.time.OffsetDateTime;
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
    public ResponseEntity<?> updateDeviceName(
            @AuthenticationPrincipal User currentUser,
            @PathVariable("deviceId") UUID deviceId,
            @Valid @RequestBody DeviceRenameRequest request
    ) {
        return ResponseEntity.ok(deviceService.updateDeviceName(currentUser.getId(), deviceId, request.getName()));
    }

    @PostMapping("/{deviceId}/remove-requests")
    public ResponseEntity<?> requestDeviceRemoval(
            @AuthenticationPrincipal User currentUser,
            @PathVariable("deviceId") UUID deviceId
    ) {
        deviceService.requestDeviceRemoval(currentUser.getId(), deviceId);
        return ResponseEntity.ok(Map.of("message", "Đã gửi yêu cầu gỡ bỏ thiết bị. Vui lòng chờ Admin xác nhận thu hồi."));
    }

    @PostMapping("/requests")
    public ResponseEntity<?> requestNewDevice(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody DeviceRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(deviceService.addDevice(request, currentUser.getId()));
    }

    @GetMapping("/{deviceId}/telemetry")
    public ResponseEntity<?> getTelemetry(
            @AuthenticationPrincipal User currentUser,
            @PathVariable("deviceId") UUID deviceId,
            @RequestParam("start_time") String startTime,
            @RequestParam("end_time") String endTime,
            @RequestParam(value = "aggregate", required = false) String aggregate) {
        OffsetDateTime start = OffsetDateTime.parse(startTime);
        OffsetDateTime end = OffsetDateTime.parse(endTime);
        return ResponseEntity.ok(telemetryService.getTelemetry(currentUser.getId(), deviceId, start.toLocalDateTime(), end.toLocalDateTime(), aggregate));
    }

    @PostMapping("/{deviceId}/command")
    public ResponseEntity<?> sendCommand(
            @AuthenticationPrincipal User currentUser,
            @PathVariable("deviceId") UUID deviceId,
            @RequestBody Map<String, String> request
    ) {
        deviceService.assertDeviceOwnership(currentUser.getId(), deviceId);
        String command = request.get("command");
        if (command == null || command.isBlank()) {
            throw new IllegalStateException("Trường command không được để trống");
        }
        irrigationContext.executeControl(manualStrategy, null, deviceId, command);
        return ResponseEntity.ok(Map.of("message", "Lệnh [" + command + "] đã được gửi tới thiết bị thành công."));
    }

    @PatchMapping("/{deviceId}/mode")
    public ResponseEntity<?> changeMode(
            @AuthenticationPrincipal User currentUser,
            @PathVariable("deviceId") UUID deviceId,
            @RequestBody Map<String, String> request
    ) {
        String modeRaw = request.get("operating_mode");
        if (modeRaw == null || modeRaw.isBlank()) {
            modeRaw = request.get("mode");
        }
        if (modeRaw == null || modeRaw.isBlank()) {
            throw new IllegalStateException("Trường operating_mode không được để trống");
        }

        OperatingModeEnum mode;
        try {
            mode = OperatingModeEnum.valueOf(modeRaw);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("Giá trị operating_mode không hợp lệ: " + modeRaw);
        }

        deviceService.changeMode(currentUser.getId(), deviceId, mode);
        return ResponseEntity.ok(Map.of("message", "Đã chuyển thiết bị sang chế độ " + mode + "."));
    }
}
