package com.yoloFarm.api;

import com.yoloFarm.api.dto.SensorData;
import com.yoloFarm.api.repository.TelemetryDataRepository;
import com.yoloFarm.api.service.mqtt.observer.DatabaseLoggerObserver;
import com.yoloFarm.api.service.mqtt.observer.WebSocketNotifierObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TelemetryObserversTest {

    @Mock
    private TelemetryDataRepository telemetryDataRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private DatabaseLoggerObserver databaseLoggerObserver;
    private WebSocketNotifierObserver webSocketNotifierObserver;

    @BeforeEach
    public void setUp() {
        databaseLoggerObserver = new DatabaseLoggerObserver(telemetryDataRepository);
        // WebSocketNotifierObserver không cần DeviceRepository nữa!
        webSocketNotifierObserver = new WebSocketNotifierObserver(messagingTemplate);
    }

    @Test
    void shouldPersistTelemetry_whenDatabaseLoggerObserverReceivesData() {
        UUID deviceId = UUID.randomUUID();
        UUID farmId = UUID.randomUUID();

        SensorData data = new SensorData(farmId, deviceId, "HUMIDITY", 65.5f, Instant.now());
        databaseLoggerObserver.update(data);

        // Xác nhận repository.save() đã được gọi để ném Data vào Database
        verify(telemetryDataRepository, times(1)).save(any());
    }

    @Test
    void shouldBroadcastTelemetryToFarmTopic_whenWebSocketObserverReceivesData() {
        UUID deviceId = UUID.randomUUID();
        UUID farmId = UUID.randomUUID();

        // farmId đã có sẵn trong SensorData → không cần mock Device/Farm nữa!
        SensorData data = new SensorData(farmId, deviceId, "TEMPERATURE", 30.2f, Instant.now());
        webSocketNotifierObserver.update(data);

        // Assert: Xác nhận kênh đích chứa đúng farmId
        String expectedDestination = "/topic/farm/" + farmId + "/telemetry";

        // Khẳng định Spring STOMP Template đã rải thông điệp JSON lên đúng kênh đích
        // này
        verify(messagingTemplate, times(1)).convertAndSend(eq(expectedDestination), (Object) any(java.util.Map.class));
    }
}
