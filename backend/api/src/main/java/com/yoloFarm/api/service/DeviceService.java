package com.yoloFarm.api.service;

import com.yoloFarm.api.dto.request.DeviceRequest;
import com.yoloFarm.api.dto.response.DeviceResponse;
import com.yoloFarm.api.entity.Device;
import com.yoloFarm.api.entity.DeviceModel;
import com.yoloFarm.api.entity.Farm;
import com.yoloFarm.api.enums.ConnectionStatusEnum;
import com.yoloFarm.api.enums.DeviceStatusEnum;
import com.yoloFarm.api.enums.OperatingModeEnum;
import com.yoloFarm.api.repository.DeviceModelRepository;
import com.yoloFarm.api.repository.DeviceRepository;
import com.yoloFarm.api.repository.FarmRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final FarmRepository farmRepository;
    private final DeviceModelRepository deviceModelRepository;
    private final NotificationService notificationService;

    public List<DeviceResponse> getDevicesByFarmId(UUID farmId, UUID ownerId) {
        if (!farmRepository.existsByIdAndOwnerId(farmId, ownerId)) {
            throw new AccessDeniedException("Bạn không có quyền truy cập danh sách thiết bị của nông trại này");
        }

        return deviceRepository.findByFarmIdAndFarmOwnerId(farmId, ownerId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public DeviceResponse addDevice(DeviceRequest request, UUID ownerId) {
        Farm farm = farmRepository.findByIdAndOwnerId(request.getFarmId(), ownerId)
                .orElseThrow(() -> new EntityNotFoundException("Farm not found with id: " + request.getFarmId()));

        DeviceModel model = deviceModelRepository.findById(request.getModelId())
                .orElseThrow(() -> new EntityNotFoundException("DeviceModel not found with id: " + request.getModelId()));

        Device device = Device.builder()
                .farm(farm)
                .model(model)
                .name(request.getName())
                .status(DeviceStatusEnum.PENDING)
                .connectionStatus(ConnectionStatusEnum.OFFLINE)
                .operatingMode(OperatingModeEnum.MANUAL)
                .build();

        device = deviceRepository.save(device);
        return mapToResponse(device);
    }

    @Transactional
    public DeviceResponse updateDeviceName(UUID ownerId, UUID deviceId, String newName) {
        Device device = deviceRepository.findByIdAndFarmOwnerId(deviceId, ownerId)
                .orElseThrow(() -> new EntityNotFoundException("Device not found with id: " + deviceId));

        if (newName != null && !newName.isBlank()) {
            device.setName(newName);
        }

        device = deviceRepository.save(device);
        return mapToResponse(device);
    }

    public void assertDeviceOwnership(UUID ownerId, UUID deviceId) {
        if (deviceRepository.findByIdAndFarmOwnerId(deviceId, ownerId).isEmpty()) {
            throw new AccessDeniedException("Bạn không có quyền thao tác thiết bị này");
        }
    }

    @Transactional
    public DeviceResponse requestDeviceRemoval(UUID ownerId, UUID deviceId) {
        Device device = deviceRepository.findByIdAndFarmOwnerId(deviceId, ownerId)
                .orElseThrow(() -> new EntityNotFoundException("Device not found with id: " + deviceId));
        device.setStatus(DeviceStatusEnum.PENDING_REMOVAL);
        return mapToResponse(deviceRepository.save(device));
    }

    @Transactional
    public DeviceResponse changeMode(UUID ownerId, UUID deviceId, OperatingModeEnum mode) {
        Device device = deviceRepository.findByIdAndFarmOwnerId(deviceId, ownerId)
                .orElseThrow(() -> new EntityNotFoundException("Device not found with id: " + deviceId));
        device.setOperatingMode(mode);
        return mapToResponse(deviceRepository.save(device));
    }

    public List<DeviceResponse> getDeviceRequests(String status) {
        List<Device> devices;
        if (status == null || status.isBlank()) {
            devices = deviceRepository.findByStatus(DeviceStatusEnum.PENDING);
        } else {
            devices = deviceRepository.findByStatus(DeviceStatusEnum.valueOf(status));
        }
        return devices.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional
    public DeviceResponse approveDevice(UUID deviceId, String adafruitFeedKey) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new EntityNotFoundException("Device not found with id: " + deviceId));

        String resolvedFeedKey = (adafruitFeedKey == null || adafruitFeedKey.isBlank())
            ? generateAutoFeedKey(device)
            : normalizeFeedKey(adafruitFeedKey);

        device.setAdafruitFeedKey(resolvedFeedKey);
        device.setStatus(DeviceStatusEnum.ACTIVE);
        Device saved = deviceRepository.save(device);

        UUID ownerId = saved.getFarm().getOwner().getId();
        notificationService.createSystemNotification(ownerId, "Thiết bị [" + saved.getName() + "] đã được duyệt và kích hoạt.");
        return mapToResponse(saved);
    }

    @Transactional
    public DeviceResponse rejectDevice(UUID deviceId, String rejectReason) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new EntityNotFoundException("Device not found with id: " + deviceId));
        device.setStatus(DeviceStatusEnum.REJECTED);
        Device saved = deviceRepository.save(device);

        UUID ownerId = saved.getFarm().getOwner().getId();
        String suffix = (rejectReason == null || rejectReason.isBlank()) ? "" : " Lý do: " + rejectReason;
        notificationService.createSystemNotification(ownerId, "Yêu cầu thiết bị [" + saved.getName() + "] đã bị từ chối." + suffix);
        return mapToResponse(saved);
    }

    @Transactional
    public void deleteDevice(UUID deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new EntityNotFoundException("Device not found with id: " + deviceId));
        deviceRepository.delete(device);
    }

    private String normalizeFeedKey(String rawFeedKey) {
        String cleaned = rawFeedKey.trim();
        String marker = "/feeds/";
        int markerIndex = cleaned.indexOf(marker);
        if (markerIndex >= 0) {
            return cleaned.substring(markerIndex + marker.length());
        }
        return cleaned;
    }

    private String generateAutoFeedKey(Device device) {
        String deviceSegment = device.getId().toString().replace("-", "").substring(0, 12);
        String ownerSegment = device.getFarm().getOwner().getId().toString().replace("-", "").substring(0, 8);
        String metricSegment = device.getModel().getMetricType().name().toLowerCase();
        return "u" + ownerSegment + "-d" + deviceSegment + "-" + metricSegment;
    }

    private DeviceResponse mapToResponse(Device device) {
        DeviceResponse response = new DeviceResponse();
        response.setId(device.getId());
        response.setFarmId(device.getFarm().getId());
        response.setModelId(device.getModel().getId());
        response.setName(device.getName());
        response.setStatus(device.getStatus());
        response.setAdafruitFeedKey(device.getAdafruitFeedKey());
        response.setConnectionStatus(device.getConnectionStatus());
        response.setLastSeen(device.getLastSeen());
        response.setOperatingMode(device.getOperatingMode());
        response.setIsActive(device.getStatus() == DeviceStatusEnum.ACTIVE);
        return response;
    }
}
