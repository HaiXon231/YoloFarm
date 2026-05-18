package com.yoloFarm.api.service;

import com.yoloFarm.api.entity.Device;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeviceRealtimeService {

    private final SimpMessagingTemplate messagingTemplate;

    public void publishDeviceState(Device device) {
        if (device == null || device.getFarm() == null) {
            return;
        }
        publishDeviceStates(device.getFarm().getId(), List.of(device));
    }

    public void publishDeviceStates(UUID farmId, List<Device> devices) {
        if (farmId == null || devices == null || devices.isEmpty()) {
            return;
        }

        List<Map<String, Object>> payload = devices.stream()
                .map(this::toPayload)
                .toList();
        messagingTemplate.convertAndSend("/topic/farm/" + farmId + "/device-status", payload);
        messagingTemplate.convertAndSend("/topic/admin/stats-changed", (Object) Map.of("reason", "device_state_changed"));
    }

    public void publishDeviceRemoved(UUID farmId, UUID deviceId) {
        if (farmId == null || deviceId == null) {
            return;
        }
        messagingTemplate.convertAndSend("/topic/farm/" + farmId + "/device-status",
                List.of(Map.<String, Object>of(
                        "deviceId", deviceId.toString(),
                        "status", "REMOVED"
                )));
        messagingTemplate.convertAndSend("/topic/admin/stats-changed", (Object) Map.of("reason", "device_removed"));
    }

    private Map<String, Object> toPayload(Device device) {
        return Map.of(
                "deviceId", device.getId().toString(),
                "connectionStatus", device.getConnectionStatus() == null ? "" : device.getConnectionStatus().name(),
                "operatingMode", device.getOperatingMode() == null ? "" : device.getOperatingMode().name(),
                "status", device.getStatus() == null ? "" : device.getStatus().name(),
                "isActive", Boolean.TRUE.equals(device.getIsActive())
        );
    }
}
