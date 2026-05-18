package com.yoloFarm.api.service;

import com.yoloFarm.api.entity.Device;
import com.yoloFarm.api.enums.ConnectionStatusEnum;
import com.yoloFarm.api.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class DeviceHeartbeatService {

    private final DeviceRepository deviceRepository;
    private final DeviceRealtimeService deviceRealtimeService;

    @Scheduled(cron = "30 * * * * *")
    @Transactional
    public void cleanupStaleConnections() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(5);
        log.debug("HeartbeatJanitor: Checking for stale devices (threshold: {})", threshold);

        try {
            List<Device> staleDevices = deviceRepository.findStaleOnlineDevices(threshold);
            if (staleDevices.isEmpty()) {
                log.debug("HeartbeatJanitor: No stale devices found.");
                return;
            }

            Map<UUID, List<Device>> byFarm = staleDevices.stream()
                    .collect(Collectors.groupingBy(d -> d.getFarm().getId()));

            deviceRepository.markStaleDevicesAsOffline(threshold);
            log.info("HeartbeatJanitor: Marked {} device(s) OFFLINE across {} farm(s).",
                    staleDevices.size(), byFarm.size());

            byFarm.forEach((farmId, devices) -> {
                devices.forEach(device -> device.setConnectionStatus(ConnectionStatusEnum.OFFLINE));
                deviceRealtimeService.publishDeviceStates(farmId, devices);
                log.debug("HeartbeatJanitor: Pushed offline event for farm {}: {} device(s).",
                        farmId, devices.size());
            });
        } catch (Exception e) {
            log.error("HeartbeatJanitor: Error updating offline status", e);
        }
    }
}
