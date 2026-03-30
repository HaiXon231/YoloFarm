package com.yoloFarm.api;

import com.yoloFarm.api.dto.SensorData;
import com.yoloFarm.api.entity.Device;
import com.yoloFarm.api.entity.Farm;
import com.yoloFarm.api.entity.Rule;
import com.yoloFarm.api.enums.ActionCommandEnum;
import com.yoloFarm.api.enums.OperatingModeEnum;
import com.yoloFarm.api.repository.DeviceRepository;
import com.yoloFarm.api.repository.RuleRepository;
import com.yoloFarm.api.service.NotificationService;
import com.yoloFarm.api.service.mqtt.MqttSenderService;
import com.yoloFarm.api.service.mqtt.observer.RuleEngineObserver;
import com.yoloFarm.api.service.strategy.AutoThresholdStrategy;
import com.yoloFarm.api.service.strategy.IrrigationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RuleEngineLogicTest {

    @Mock
    private RuleRepository ruleRepository;

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private MqttSenderService mqttSenderService;

    @Mock
    private NotificationService notificationService;

    private IrrigationContext irrigationContext;
    private AutoThresholdStrategy autoThresholdStrategy;
    private RuleEngineObserver ruleEngineObserver;

    @BeforeEach
    public void setUp() {
        // 1. Tự tay lắp ráp các hạt nhân Logic (Không cần chạy nguyên cả Server Spring Boot nặng nề)
        autoThresholdStrategy = new AutoThresholdStrategy(deviceRepository, mqttSenderService);
        irrigationContext = new IrrigationContext();
        ruleEngineObserver = new RuleEngineObserver(ruleRepository, irrigationContext, autoThresholdStrategy, notificationService);
    }

    @Test
    public void testRuleEngineTriggersAutoIrrigation() {
        // [CHUẨN BỊ] - KHỞI TẠO MOCK DATA
        UUID sensorId = UUID.randomUUID();
        UUID pumpId = UUID.randomUUID();
        UUID farmId = UUID.randomUUID();

        // Máy bơm (Sẽ bị kích hoạt)
        Device mockPump = new Device();
        mockPump.setId(pumpId);
        mockPump.setOperatingMode(OperatingModeEnum.AUTO); // Bắt buộc phải là AUTO mới chạy
        mockPump.setAdafruitFeedKey("pump-feed-01");

        Farm mockFarm = new Farm();
        mockFarm.setId(farmId);

        // Luật: ĐỘ ẨM MÀ BÉ HƠN 40 THÌ BẮT ĐẦU BƠM (Lệnh ON)
        Rule mockRule = new Rule();
        mockRule.setFarm(mockFarm);
        mockRule.setTriggerDevice(new Device());
        mockRule.setActionDevice(mockPump);
        mockRule.setOperator("<");
        mockRule.setThresholdValue(40.0f);
        mockRule.setActionCommand(ActionCommandEnum.ON);

        // Hướng dẫn Mockito: Nếu truy vấn Database bằng sensorId -> Trả về mockRule
        when(ruleRepository.findActiveRulesWithAssociations(sensorId))
                .thenReturn(List.of(mockRule));
        
        // Hướng dẫn Mockito: Nếu Strategy tìm máy bơm trong Database -> Trả về mockPump
        when(deviceRepository.findById(pumpId))
                .thenReturn(Optional.of(mockPump));

        // [THỰC THI] - CHẠY TEST
        // Đóng vai trò Cảm biến vừa gửi về độ ẩm = 35.5 (Tức là Bé hơn 40 -> Cần tưới)
        SensorData data = new SensorData(farmId, sensorId, "SOIL_MOISTURE", 35.5f, Instant.now());
        ruleEngineObserver.update(data);

        // [KIỂM TRA] - ASSERTION
        // Khẳng định chắc chắn 100% rằng hệ thống ĐÃ RA LỆNH "ON" ở kênh "pump-feed-01". 
        verify(mqttSenderService, times(1)).sendCommand("pump-feed-01", "ON");
    }

    @Test
    public void testRuleEngineIgnoresWhenConditionNotMet() {
        // [CHUẨN BỊ]
        UUID sensorId = UUID.randomUUID();
        UUID farmId = UUID.randomUUID();

        Rule mockRule = new Rule();
        mockRule.setOperator("<");
        mockRule.setThresholdValue(40.0f);
        
        when(ruleRepository.findActiveRulesWithAssociations(sensorId))
                .thenReturn(List.of(mockRule));
        
        // [THỰC THI]
        // Đóng vai Cảm biến gửi về độ ẩm = 50.0 (Lớn hơn 40 -> Không cần tưới, đất vẫn ẩm)
        SensorData data = new SensorData(farmId, sensorId, "SOIL_MOISTURE", 50.0f, Instant.now());
        ruleEngineObserver.update(data);
        
        // [KIỂM TRA]
        // Khẳng định hàm gửi lệnh của MqttSender KHÔNG BAO GIỜ bị gọi.
        verify(mqttSenderService, never()).sendCommand(any(), any());
    }
}
