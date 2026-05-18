package com.yoloFarm.api.service.automation;

import com.yoloFarm.api.entity.Device;
import com.yoloFarm.api.entity.Farm;
import com.yoloFarm.api.entity.User;
import com.yoloFarm.api.repository.DeviceRepository;
import com.yoloFarm.api.service.AutomationConfigService;
import com.yoloFarm.api.service.NotificationService;
import com.yoloFarm.api.service.strategy.AutoThresholdStrategy;
import com.yoloFarm.api.service.strategy.IrrigationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutoIrrigationSafetyServiceTest {

    @Mock
    private DeviceRepository deviceRepository;
    @Mock
    private IrrigationContext irrigationContext;
    @Mock
    private AutoThresholdStrategy autoThresholdStrategy;
    @Mock
    private NotificationService notificationService;
    @Mock
    private AutomationRuntimeStateService automationRuntimeStateService;
    @Mock
    private AutomationConfigService automationConfigService;

    @InjectMocks
    private AutoIrrigationSafetyService safetyService;

    private Clock fixedClock;
    private Instant now;

    @BeforeEach
    void setUp() {
        now = Instant.parse("2026-04-27T10:00:00Z");
        fixedClock = Clock.fixed(now, ZoneId.of("UTC"));
        ReflectionTestUtils.setField(safetyService, "clock", fixedClock);
        when(automationConfigService.getMaxAutoOnMinutes()).thenReturn(20);
    }

    @Test
    void enforceAutoOffSafety_WhenNotExceeded_ShouldDoNothing() {
        UUID deviceId = UUID.randomUUID();
        Device device = new Device();
        device.setId(deviceId);

        when(deviceRepository.findActiveAutoActuatorsWithFarmAndOwner()).thenReturn(List.of(device));
        // Started 10 minutes ago, limit is 20
        when(automationRuntimeStateService.getAutoOnSince(deviceId))
                .thenReturn(now.minus(10, ChronoUnit.MINUTES));

        safetyService.enforceAutoOffSafety();

        verify(irrigationContext, never()).executeControl(any(), any(), any(), any());
    }

    @Test
    void enforceAutoOffSafety_WhenExceeded_ShouldForceOff() {
        UUID ownerId = UUID.randomUUID();
        User owner = new User();
        owner.setId(ownerId);

        UUID farmId = UUID.randomUUID();
        Farm farm = new Farm();
        farm.setId(farmId);
        farm.setOwner(owner);

        UUID deviceId = UUID.randomUUID();
        Device device = new Device();
        device.setId(deviceId);
        device.setFarm(farm);
        device.setName("Pump A");

        when(deviceRepository.findActiveAutoActuatorsWithFarmAndOwner()).thenReturn(List.of(device));
        // Started 30 minutes ago, limit is 20
        when(automationRuntimeStateService.getAutoOnSince(deviceId))
                .thenReturn(now.minus(30, ChronoUnit.MINUTES));
        when(irrigationContext.executeControl(autoThresholdStrategy, farmId, deviceId, "OFF"))
                .thenReturn(true);

        safetyService.enforceAutoOffSafety();

        verify(irrigationContext).executeControl(autoThresholdStrategy, farmId, deviceId, "OFF");
        verify(automationRuntimeStateService).markAutoCommand(deviceId, "OFF", now);
        verify(notificationService).createSystemNotification(eq(ownerId), contains("tắt tự động sau 30 phút"));
    }

    @Test
    void enforceAutoOffSafety_WhenAutoOnSinceNull_ShouldMarkCurrentTime() {
        UUID deviceId = UUID.randomUUID();
        Device device = new Device();
        device.setId(deviceId);

        when(deviceRepository.findActiveAutoActuatorsWithFarmAndOwner()).thenReturn(List.of(device));
        when(automationRuntimeStateService.getAutoOnSince(deviceId)).thenReturn(null);

        safetyService.enforceAutoOffSafety();

        verify(automationRuntimeStateService).markAutoCommand(deviceId, "ON", now);
        verify(irrigationContext, never()).executeControl(any(), any(), any(), any());
    }
}
