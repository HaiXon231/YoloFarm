package com.yoloFarm.api;

import com.yoloFarm.api.dto.SensorData;
import com.yoloFarm.api.entity.Device;
import com.yoloFarm.api.entity.DeviceModel;
import com.yoloFarm.api.entity.Farm;
import com.yoloFarm.api.enums.DeviceTypeEnum;
import com.yoloFarm.api.enums.MetricTypeEnum;
import com.yoloFarm.api.repository.DeviceRepository;
import com.yoloFarm.api.service.DeviceRealtimeService;
import com.yoloFarm.api.service.NotificationService;
import com.yoloFarm.api.service.automation.AutomationRuntimeStateService;
import com.yoloFarm.api.service.mqtt.MqttReceiverService;
import com.yoloFarm.api.service.mqtt.observer.Observer;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MqttReceiverServiceTest {
    private org.springframework.jdbc.core.JdbcTemplate mockJdbcTemplate;

    private MqttReceiverService mqttReceiverService;
    private Observer mockObserver;
    private DeviceRepository mockDeviceRepo;
    private NotificationService mockNotificationService;
    private AutomationRuntimeStateService mockAutomationRuntimeStateService;
    private DeviceRealtimeService mockDeviceRealtimeService;
    private IMqttClient mockMqttClient;

    @BeforeEach
    public void setUp() {
        mockObserver = Mockito.mock(Observer.class);
        mockDeviceRepo = Mockito.mock(DeviceRepository.class);
        mockNotificationService = Mockito.mock(NotificationService.class);
        mockAutomationRuntimeStateService = Mockito.mock(AutomationRuntimeStateService.class);
        mockDeviceRealtimeService = Mockito.mock(DeviceRealtimeService.class);
        mockMqttClient = Mockito.mock(IMqttClient.class);
        mockJdbcTemplate = Mockito.mock(org.springframework.jdbc.core.JdbcTemplate.class);

        mqttReceiverService = new MqttReceiverService(List.of(mockObserver), mockMqttClient, mockDeviceRepo,
                mockNotificationService, mockAutomationRuntimeStateService, mockDeviceRealtimeService, mockJdbcTemplate);
        mqttReceiverService.init();
    }

    @Test
    void shouldNotifyObservers_whenMessageMatchesKnownFeed() throws Exception {
        UUID deviceId = UUID.randomUUID();
        UUID farmId = UUID.randomUUID();

        Farm mockFarm = new Farm();
        mockFarm.setId(farmId);

        DeviceModel mockModel = new DeviceModel();
        mockModel.setDeviceType(DeviceTypeEnum.SENSOR);
        mockModel.setMetricType(MetricTypeEnum.TEMP);

        Device mockDevice = new Device();
        mockDevice.setId(deviceId);
        mockDevice.setModel(mockModel);
        mockDevice.setFarm(mockFarm);

        when(mockDeviceRepo.findByAdafruitFeedKeyIgnoreCaseWithModelAndFarm("temp-feed"))
                .thenReturn(Optional.of(mockDevice));

        MqttMessage mqttMsg = new MqttMessage("37.5".getBytes());
        mqttReceiverService.messageArrived("testuser/feeds/temp-feed", mqttMsg);

        verify(mockObserver, timeout(2000).times(1)).update(any(SensorData.class));
    }

    @Test
    void shouldResolveDashUnderscoreAlias_whenPrimaryFeedLookupMisses() throws Exception {
        UUID deviceId = UUID.randomUUID();
        UUID farmId = UUID.randomUUID();

        Farm mockFarm = new Farm();
        mockFarm.setId(farmId);

        DeviceModel mockModel = new DeviceModel();
        mockModel.setDeviceType(DeviceTypeEnum.SENSOR);
        mockModel.setMetricType(MetricTypeEnum.SOIL_MOISTURE);

        Device mockDevice = new Device();
        mockDevice.setId(deviceId);
        mockDevice.setModel(mockModel);
        mockDevice.setFarm(mockFarm);

        when(mockDeviceRepo.findByAdafruitFeedKeyIgnoreCaseWithModelAndFarm("oil-zone-b"))
                .thenReturn(Optional.empty());
        when(mockDeviceRepo.findByAdafruitFeedKeyIgnoreCaseWithModelAndFarm("oil_zone_b"))
                .thenReturn(Optional.of(mockDevice));

        MqttMessage mqttMsg = new MqttMessage("41.2".getBytes());
        mqttReceiverService.messageArrived("testuser/feeds/oil-zone-b", mqttMsg);

        verify(mockObserver, timeout(2000).times(1)).update(any(SensorData.class));
    }
}
