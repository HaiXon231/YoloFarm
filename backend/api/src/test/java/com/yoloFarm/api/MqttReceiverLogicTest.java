package com.yoloFarm.api;

import com.yoloFarm.api.service.mqtt.MqttReceiverService;
import com.yoloFarm.api.service.mqtt.observer.Observer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class MqttReceiverLogicTest {

    private MqttReceiverService mqttReceiverService;
    private Observer mockObserver;

    @BeforeEach
    public void setUp() {
        mockObserver = Mockito.mock(Observer.class);
        mqttReceiverService = new MqttReceiverService(List.of(mockObserver));
        mqttReceiverService.init(); // Kích hoạt @PostConstruct để nạp observer
    }

    @Test
    public void testMqttMessageNotifiesObservers() {
        // [CHUẨN BỊ] - Payload JSON giả lập từ thiết bị
        UUID deviceId = UUID.randomUUID();
        String jsonPayload = String.format("""
                {
                   "deviceId": "%s",
                   "metricType": "TEMPERATURE",
                   "value": 37.5
                }
                """, deviceId);

        // [THỰC THI] - Giả lập Broker nhận tin
        mqttReceiverService.simulateMessageReceived("sensors/temperature", jsonPayload);

        // [KIỂM TRA] - Đảm bảo Observer con cưng đã được System gọi `update` và tuồn Data vào
        verify(mockObserver, times(1)).update(eq(deviceId), eq("TEMPERATURE"), eq(37.5f));
    }
}
