package com.yoloFarm.api.service;

import com.yoloFarm.api.dto.request.AutomationConfigRequest;
import com.yoloFarm.api.dto.request.DeviceModelRequest;
import com.yoloFarm.api.dto.response.*;
import com.yoloFarm.api.enums.DeviceStatusEnum;
import com.yoloFarm.api.enums.RoleEnum;
import com.yoloFarm.api.repository.DeviceRepository;
import com.yoloFarm.api.repository.FarmRepository;
import com.yoloFarm.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private final DeviceModelService deviceModelService;
    private final DeviceService deviceService;
    private final UserRepository userRepository;
    private final FarmRepository farmRepository;
    private final DeviceRepository deviceRepository;
    private final IMqttClient mqttClient;
    private final AutomationConfigService automationConfigService;

    public AdminStatsResponse getStats() {
        return AdminStatsResponse.builder()
                .totalFarmers(userRepository.countByRole(RoleEnum.FARMER))
                .totalFarms(farmRepository.count())
                .totalDevices(deviceRepository.count())
                .pendingRequests(
                        deviceRepository.countByStatus(DeviceStatusEnum.PENDING)
                                + deviceRepository.countByStatus(DeviceStatusEnum.PENDING_REMOVAL))
                .activeDevices(
                        deviceRepository.countByConnectionStatus(com.yoloFarm.api.enums.ConnectionStatusEnum.ONLINE))
                .apiStatus(true)
                .mqttStatus(mqttClient.isConnected())
                .dbStatus(true)
                .build();
    }

    public List<AdminFarmerResponse> getFarmers() {
        return userRepository.findAdminFarmerSummaries(RoleEnum.FARMER).stream()
                .map(p -> AdminFarmerResponse.builder()
                        .id(p.getId())
                        .username(p.getUsername())
                        .email(p.getEmail())
                        .createdAt(p.getCreatedAt())
                        .farmCount(p.getFarmCount())
                        .build())
                .toList();
    }

    public List<AdminFarmResponse> getFarms() {
        return farmRepository.findAdminFarmSummaries().stream()
                .map(p -> AdminFarmResponse.builder()
                        .id(p.getId())
                        .name(p.getName())
                        .location(p.getLocation())
                        .ownerName(p.getOwnerName())
                        .ownerEmail(p.getOwnerEmail())
                        .createdAt(p.getCreatedAt())
                        .deviceCount(p.getDeviceCount())
                        .build())
                .toList();
    }

    public List<AdminDeviceResponse> getDevices() {
        return deviceRepository.findAdminDeviceSummaries().stream()
                .map(p -> AdminDeviceResponse.builder()
                        .id(p.getId())
                        .name(p.getName())
                        .modelName(p.getModelName())
                        .deviceType(p.getDeviceType())
                        .status(p.getStatus())
                        .farmName(p.getFarmName())
                        .ownerName(p.getOwnerName())
                        .connectionStatus(p.getConnectionStatus())
                        .isActive(Boolean.TRUE.equals(p.getIsActive()))
                        .build())
                .toList();
    }

    @Transactional
    public DeviceModelResponse createDeviceModel(DeviceModelRequest request) {
        return deviceModelService.createDeviceModel(request);
    }

    @Transactional
    public void deleteDevice(UUID deviceId) {
        deviceService.deleteDevice(deviceId);
    }

    @Transactional
    public AutomationConfigResponse getAutomationConfig() {
        return automationConfigService.getConfig();
    }

    @Transactional
    public AutomationConfigResponse updateAutomationConfig(AutomationConfigRequest request) {
        return automationConfigService.updateConfig(request);
    }

    public List<DeviceResponse> getDeviceRequests(String status) {
        return deviceService.getDeviceRequests(status);
    }

    @Transactional
    public DeviceResponse approveDevice(UUID deviceId, String adafruitFeedKey) {
        return deviceService.approveDevice(deviceId, adafruitFeedKey);
    }

    @Transactional
    public Map<String, String> rejectDevice(UUID deviceId, String rejectReason) {
        deviceService.rejectDevice(deviceId, rejectReason);
        return Map.of("message", "Đã từ chối yêu cầu và gửi thông báo cho Nông dân.");
    }
}
