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
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final FarmRepository farmRepository;
    private final DeviceModelRepository deviceModelRepository;

    public List<DeviceResponse> getDevicesByFarmId(UUID farmId) {
        return deviceRepository.findByFarmId(farmId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public DeviceResponse addDevice(DeviceRequest request) {
        Farm farm = farmRepository.findById(request.getFarmId())
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

    public DeviceResponse updateDevice(UUID deviceId, String newName, DeviceStatusEnum newStatus) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new EntityNotFoundException("Device not found with id: " + deviceId));

        if (newName != null && !newName.isBlank()) {
            device.setName(newName);
        }
        if (newStatus != null) {
            device.setStatus(newStatus);
        }
        
        device = deviceRepository.save(device);
        return mapToResponse(device);
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
