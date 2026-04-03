package com.yoloFarm.api;

import com.yoloFarm.api.entity.Device;
import com.yoloFarm.api.entity.DeviceModel;
import com.yoloFarm.api.entity.Farm;
import com.yoloFarm.api.entity.User;
import com.yoloFarm.api.enums.ConnectionStatusEnum;
import com.yoloFarm.api.enums.DeviceStatusEnum;
import com.yoloFarm.api.enums.DeviceTypeEnum;
import com.yoloFarm.api.enums.MetricTypeEnum;
import com.yoloFarm.api.enums.OperatingModeEnum;
import com.yoloFarm.api.dto.response.DeviceResponse;
import com.yoloFarm.api.repository.DeviceModelRepository;
import com.yoloFarm.api.repository.DeviceRepository;
import com.yoloFarm.api.repository.FarmRepository;
import com.yoloFarm.api.repository.RuleRepository;
import com.yoloFarm.api.service.AdafruitApiService;
import com.yoloFarm.api.service.DeviceService;
import com.yoloFarm.api.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceServiceRenameSyncTest {

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private FarmRepository farmRepository;

    @Mock
    private DeviceModelRepository deviceModelRepository;

    @Mock
    private RuleRepository ruleRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AdafruitApiService adafruitApiService;

    @InjectMocks
    private DeviceService deviceService;

    private UUID ownerId;
    private UUID deviceId;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
        deviceId = UUID.randomUUID();
    }

    @Test
    void renameShouldSyncAdafruitFeedNameWhenFeedKeyExists() {
        Device device = buildDevice("Pump Zone A", "pump-zone-a");

        when(deviceRepository.findByIdAndFarmOwnerId(deviceId, ownerId)).thenReturn(Optional.of(device));
        when(deviceRepository.save(any(Device.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DeviceResponse response = deviceService.updateDeviceName(ownerId, deviceId, "  Bom Zone A Moi  ");

        verify(adafruitApiService).updateFeedName("pump-zone-a", "Bom Zone A Moi");
        assertEquals("Bom Zone A Moi", response.getName());
    }

    @Test
    void renameShouldNotSyncAdafruitWhenFeedKeyIsMissing() {
        Device device = buildDevice("Humidity Sensor", null);

        when(deviceRepository.findByIdAndFarmOwnerId(deviceId, ownerId)).thenReturn(Optional.of(device));
        when(deviceRepository.save(any(Device.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DeviceResponse response = deviceService.updateDeviceName(ownerId, deviceId, "Humidity Sensor V2");

        verify(adafruitApiService, never()).updateFeedName(any(), any());
        assertEquals("Humidity Sensor V2", response.getName());
    }

    @Test
    void renameShouldNotSyncAdafruitWhenNameNotChanged() {
        Device device = buildDevice("Temperature Sensor", "temp-zone-a");

        when(deviceRepository.findByIdAndFarmOwnerId(deviceId, ownerId)).thenReturn(Optional.of(device));
        when(deviceRepository.save(any(Device.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DeviceResponse response = deviceService.updateDeviceName(ownerId, deviceId, " Temperature Sensor ");

        verify(adafruitApiService, never()).updateFeedName(any(), any());
        assertEquals("Temperature Sensor", response.getName());
    }

    private Device buildDevice(String name, String feedKey) {
        User owner = User.builder()
                .id(ownerId)
                .username("farmer")
                .password("pwd")
                .email("farmer@yolo.test")
                .role(com.yoloFarm.api.enums.RoleEnum.FARMER)
                .build();

        Farm farm = Farm.builder()
                .id(UUID.randomUUID())
                .name("Farm A")
                .owner(owner)
                .build();

        DeviceModel model = DeviceModel.builder()
                .id(UUID.randomUUID())
                .modelName("Model A")
                .deviceType(DeviceTypeEnum.SENSOR)
                .metricType(MetricTypeEnum.HUMIDITY)
                .build();

        return Device.builder()
                .id(deviceId)
                .farm(farm)
                .model(model)
                .name(name)
                .status(DeviceStatusEnum.ACTIVE)
                .connectionStatus(ConnectionStatusEnum.ONLINE)
                .operatingMode(OperatingModeEnum.MANUAL)
                .adafruitFeedKey(feedKey)
                .isActive(true)
                .build();
    }
}
