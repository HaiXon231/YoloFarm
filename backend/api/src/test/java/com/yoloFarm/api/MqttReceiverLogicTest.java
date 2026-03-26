package com.yoloFarm.api;

import com.yoloFarm.api.dto.SensorData;
import com.yoloFarm.api.entity.Device;
import com.yoloFarm.api.entity.DeviceModel;
import com.yoloFarm.api.entity.Farm;
import com.yoloFarm.api.enums.DeviceTypeEnum;
import com.yoloFarm.api.enums.MetricTypeEnum;
import com.yoloFarm.api.repository.DeviceRepository;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MqttReceiverLogicTest {

    private MqttReceiverService mqttReceiverService;
    private Observer mockObserver;
    private DeviceRepository mockDeviceRepo;
    private IMqttClient mockMqttClient;

    @BeforeEach
    public void setUp() {
        mockObserver = Mockito.mock(Observer.class);
        mockDeviceRepo = Mockito.mock(DeviceRepository.class);
        mockMqttClient = Mockito.mock(IMqttClient.class);
        
        mqttReceiverService = new MqttReceiverService(List.of(mockObserver), mockMqttClient, mockDeviceRepo);
        mqttReceiverService.init(); // Kích hoạt @PostConstruct
    }

    @Test
    public void testMqttMessageNotifiesObservers() throws Exception {
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
        
        // Mock DB: Khi tìm kiếm bằng "temp-feed" thì trả về mockDevice
        when(mockDeviceRepo.findByAdafruitFeedKeyWithModelAndFarm("temp-feed")).thenReturn(Optional.of(mockDevice));

        // [THỰC THI] - Giả lập Adafruit IO đẩy bản tin
        MqttMessage mqttMsg = new MqttMessage("37.5".getBytes());
        mqttReceiverService.messageArrived("testuser/feeds/temp-feed", mqttMsg);

        // [KIỂM TRA] - Observer phải được gọi với SensorData chứa đúng thông tin
        verify(mockObserver, times(1)).update(any(SensorData.class));
    }
}
