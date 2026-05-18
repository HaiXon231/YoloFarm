package com.yoloFarm.api;

import com.yoloFarm.api.dto.response.DeviceResponse;
import com.yoloFarm.api.entity.Device;
import com.yoloFarm.api.entity.DeviceModel;
import com.yoloFarm.api.entity.Farm;
import com.yoloFarm.api.entity.User;
import com.yoloFarm.api.enums.ConnectionStatusEnum;
import com.yoloFarm.api.enums.DeviceStatusEnum;
import com.yoloFarm.api.enums.DeviceTypeEnum;
import com.yoloFarm.api.enums.MetricTypeEnum;
import com.yoloFarm.api.enums.OperatingModeEnum;
import com.yoloFarm.api.enums.RoleEnum;
import com.yoloFarm.api.repository.DeviceModelRepository;
import com.yoloFarm.api.repository.DeviceRepository;
import com.yoloFarm.api.repository.FarmRepository;
import com.yoloFarm.api.repository.RuleRepository;
import com.yoloFarm.api.service.AdafruitApiService;
import com.yoloFarm.api.service.DeviceRealtimeService;
import com.yoloFarm.api.service.DeviceService;
import com.yoloFarm.api.service.NotificationService;
import com.yoloFarm.api.service.automation.AutomationRuntimeStateService;
import com.yoloFarm.api.service.mqtt.MqttReceiverService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceServiceRemovalApprovalTest {

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

        @Mock
        private JdbcTemplate jdbcTemplate;

        @Mock
        private MqttReceiverService mqttReceiverService;

        @Mock
        private AutomationRuntimeStateService automationRuntimeStateService;

        @Mock
        private DeviceRealtimeService deviceRealtimeService;

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
        void shouldDeleteFeedAndNotifyRuleCleanup_whenApprovingPendingRemoval() {
                Device device = buildPendingRemovalDevice("Máy bơm zone A", "pump-zone-a");

                when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));
                when(ruleRepository.findRuleNamesBoundToDevice(deviceId))
                                .thenReturn(List.of("Tưới sáng", "Bật bơm khi nóng", "Tưới sáng"));

                DeviceResponse response = deviceService.approveDevice(deviceId, null);

                assertEquals(DeviceStatusEnum.PENDING_REMOVAL, response.getStatus());
                verify(adafruitApiService).deleteFeed("pump-zone-a");
                verify(ruleRepository).deleteRulesBoundToDevice(deviceId);
                verify(deviceRepository).delete(device);
                verify(mqttReceiverService).evictFeedKeyCache("pump-zone-a");

                verify(notificationService, times(2)).createSystemNotification(eq(ownerId), any(String.class));
                verify(notificationService).createSystemNotification(eq(ownerId),
                                eq("Yêu cầu gỡ bỏ thiết bị [Máy bơm zone A] đã được duyệt. Thiết bị đã được thu hồi khỏi hệ thống."));
                verify(notificationService).createSystemNotification(eq(ownerId),
                                eq("Các rule liên quan đến thiết bị [Máy bơm zone A] đã bị xóa theo: Tưới sáng, Bật bơm khi nóng."));
        }

        @Test
        void shouldSkipRuleCleanupNotification_whenNoRulesAreBound() {
                Device device = buildPendingRemovalDevice("Cảm biến đất", "soil-zone-a");

                when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));
                when(ruleRepository.findRuleNamesBoundToDevice(deviceId)).thenReturn(List.of());

                deviceService.approveDevice(deviceId, null);

                verify(notificationService, times(1)).createSystemNotification(eq(ownerId), any(String.class));
                verify(notificationService).createSystemNotification(eq(ownerId),
                                eq("Yêu cầu gỡ bỏ thiết bị [Cảm biến đất] đã được duyệt. Thiết bị đã được thu hồi khỏi hệ thống."));
                verify(adafruitApiService).deleteFeed("soil-zone-a");
                verify(ruleRepository).deleteRulesBoundToDevice(deviceId);
                verify(deviceRepository).delete(device);
                verify(mqttReceiverService).evictFeedKeyCache("soil-zone-a");
        }

        @Test
        void shouldSkipFeedDeletion_whenPendingRemovalDeviceHasNoFeedKey() {
                Device device = buildPendingRemovalDevice("Cảm biến nhiệt", null);

                when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(device));
                when(ruleRepository.findRuleNamesBoundToDevice(deviceId)).thenReturn(List.of("Rule 1"));

                deviceService.approveDevice(deviceId, null);

                verify(adafruitApiService, never()).deleteFeed(any());
                verify(ruleRepository).deleteRulesBoundToDevice(deviceId);
                verify(deviceRepository).delete(device);
                verify(mqttReceiverService, never()).evictFeedKeyCache(any());
        }

        private Device buildPendingRemovalDevice(String name, String feedKey) {
                User owner = User.builder()
                                .id(ownerId)
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
                                .modelName("Model A")
                                .deviceType(DeviceTypeEnum.ACTUATOR)
                                .metricType(MetricTypeEnum.PUMP)
                                .build();

                return Device.builder()
                                .id(deviceId)
                                .farm(farm)
                                .model(model)
                                .name(name)
                                .status(DeviceStatusEnum.PENDING_REMOVAL)
                                .connectionStatus(ConnectionStatusEnum.ONLINE)
                                .operatingMode(OperatingModeEnum.AUTO)
                                .adafruitFeedKey(feedKey)
                                .isActive(true)
                                .build();
        }
}
