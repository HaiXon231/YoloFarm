package com.yoloFarm.api.service.automation;

import com.yoloFarm.api.entity.Device;
import com.yoloFarm.api.repository.DeviceRepository;
import com.yoloFarm.api.service.NotificationService;
import com.yoloFarm.api.service.strategy.AutoThresholdStrategy;
import com.yoloFarm.api.service.strategy.IrrigationContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AutoIrrigationSafetyService {

    private final DeviceRepository deviceRepository;
    private final IrrigationContext irrigationContext;
    private final AutoThresholdStrategy autoThresholdStrategy;
    private final NotificationService notificationService;
    private final AutomationRuntimeStateService automationRuntimeStateService;
    private final Clock clock;

    @Value("${app.automation.max-auto-on-minutes:20}")
    private long maxAutoOnMinutes;

    @Scheduled(fixedDelayString = "${app.automation.auto-off-watchdog-interval-ms:30000}")
    public void enforceAutoOffSafety() {
        if (maxAutoOnMinutes <= 0) {
            return;
        }

        Instant now = clock.instant();
        List<Device> activeAutoActuators = deviceRepository.findActiveAutoActuatorsWithFarmAndOwner();

        for (Device device : activeAutoActuators) {
            Instant autoOnSince = automationRuntimeStateService.getAutoOnSince(device.getId());

            if (autoOnSince == null) {
                // Tránh tắt nhầm ngay khi deploy/restart: bắt đầu đếm từ thời điểm watchdog
                // thấy thiết bị đang ON.
                automationRuntimeStateService.markAutoCommand(device.getId(), "ON", now);
                continue;
            }

            long elapsedMinutes = Duration.between(autoOnSince, now).toMinutes();
            if (elapsedMinutes < maxAutoOnMinutes) {
                continue;
            }

            boolean success = irrigationContext.executeControl(
                    autoThresholdStrategy,
                    device.getFarm().getId(),
                    device.getId(),
                    "OFF");

            if (!success) {
                continue;
            }

            automationRuntimeStateService.markAutoCommand(device.getId(), "OFF", now);

            String message = String.format(
                    "Safety Auto-Off: Thiết bị [%s] đã được tắt tự động sau %d phút chạy liên tục ở chế độ AUTO.",
                    device.getName(),
                    elapsedMinutes);
            notificationService.createSystemNotification(device.getFarm().getOwner().getId(), message);

            log.warn("AutoSafety: Đã ép OFF thiết bị {} sau {} phút chạy AUTO liên tục",
                    device.getId(), elapsedMinutes);
        }
    }
}
