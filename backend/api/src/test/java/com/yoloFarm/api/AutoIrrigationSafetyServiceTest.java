package com.yoloFarm.api;

import com.yoloFarm.api.entity.Device;
import com.yoloFarm.api.entity.DeviceModel;
import com.yoloFarm.api.entity.Farm;
import com.yoloFarm.api.entity.User;
import com.yoloFarm.api.enums.DeviceStatusEnum;
import com.yoloFarm.api.enums.DeviceTypeEnum;
import com.yoloFarm.api.enums.MetricTypeEnum;
import com.yoloFarm.api.enums.OperatingModeEnum;
import com.yoloFarm.api.enums.RoleEnum;
import com.yoloFarm.api.repository.DeviceRepository;
import com.yoloFarm.api.service.AutomationConfigService;
import com.yoloFarm.api.service.NotificationService;
import com.yoloFarm.api.service.automation.AutoIrrigationSafetyService;
import com.yoloFarm.api.service.automation.AutomationRuntimeStateService;
import com.yoloFarm.api.service.strategy.AutoThresholdStrategy;
import com.yoloFarm.api.service.strategy.IrrigationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    private AutomationConfigService automationConfigService;

    private AutomationRuntimeStateService automationRuntimeStateService;
    private AutoIrrigationSafetyService autoIrrigationSafetyService;

    @BeforeEach
    void setUp() {
        automationRuntimeStateService = new AutomationRuntimeStateService();
        Clock fixedClock = Clock.fixed(Instant.parse("2026-04-10T12:00:00Z"), ZoneOffset.UTC);

        autoIrrigationSafetyService = new AutoIrrigationSafetyService(
                deviceRepository,
                irrigationContext,
                autoThresholdStrategy,
                notificationService,
                automationRuntimeStateService,
                automationConfigService,
                fixedClock);
        when(automationConfigService.getMaxAutoOnMinutes()).thenReturn(20);
    }

    @Test
    void shouldForceOff_whenAutoRuntimeExceedsThreshold() {
        Device device = buildActiveAutoActuator();
        when(deviceRepository.findActiveAutoActuatorsWithFarmAndOwner()).thenReturn(List.of(device));
        when(irrigationContext.executeControl(any(), eq(device.getFarm().getId()), eq(device.getId()), eq("OFF")))
                .thenReturn(true);

        automationRuntimeStateService.markAutoCommand(
                device.getId(),
                "ON",
                Instant.parse("2026-04-10T11:30:00Z"));

        autoIrrigationSafetyService.enforceAutoOffSafety();

        verify(irrigationContext).executeControl(any(), eq(device.getFarm().getId()), eq(device.getId()), eq("OFF"));
        verify(notificationService).createSystemNotification(eq(device.getFarm().getOwner().getId()), any());
        assertNull(automationRuntimeStateService.getAutoOnSince(device.getId()));
    }

    @Test
    void shouldInitializeOnTimestamp_whenAutoActuatorIsOnWithoutRuntimeState() {
        Device device = buildActiveAutoActuator();
        when(deviceRepository.findActiveAutoActuatorsWithFarmAndOwner()).thenReturn(List.of(device));

        autoIrrigationSafetyService.enforceAutoOffSafety();

        verify(irrigationContext, never()).executeControl(any(), any(), any(), any());
        assertNotNull(automationRuntimeStateService.getAutoOnSince(device.getId()));
    }

    private Device buildActiveAutoActuator() {
        User owner = User.builder()
                .id(UUID.randomUUID())
                .username("farmer")
                .password("pwd")
                .email("farmer@yolo.test")
                .role(RoleEnum.FARMER)
                .build();

        Farm farm = Farm.builder()
                .id(UUID.randomUUID())
                .name("Farm A")
                .owner(owner)
                .build();

        DeviceModel model = DeviceModel.builder()
                .id(UUID.randomUUID())
                .modelName("Pump Model")
                .deviceType(DeviceTypeEnum.ACTUATOR)
                .metricType(MetricTypeEnum.PUMP)
                .build();

        return Device.builder()
                .id(UUID.randomUUID())
                .farm(farm)
                .model(model)
                .name("Pump A")
                .status(DeviceStatusEnum.ACTIVE)
                .operatingMode(OperatingModeEnum.AUTO)
                .isActive(true)
                .adafruitFeedKey("pump-a")
                .build();
    }
}
