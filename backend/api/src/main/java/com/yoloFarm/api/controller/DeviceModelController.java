package com.yoloFarm.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.yoloFarm.api.service.DeviceModelService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/device-models")
@RequiredArgsConstructor
public class DeviceModelController {
    private final DeviceModelService deviceModelService;

    @GetMapping
    public ResponseEntity<?> getDeviceModels() {
        return ResponseEntity.ok(null);
    }
}
