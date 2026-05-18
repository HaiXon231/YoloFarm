package com.yoloFarm.api.service.strategy;

import com.yoloFarm.api.entity.Device;
import com.yoloFarm.api.enums.OperatingModeEnum;
import com.yoloFarm.api.repository.DeviceRepository;
import com.yoloFarm.api.service.DeviceRealtimeService;
import com.yoloFarm.api.service.mqtt.MqttSenderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class AutoThresholdStrategy implements IrrigationStrategy {

    private final DeviceRepository deviceRepository;
    private final MqttSenderService mqttSenderService;
    private final DeviceRealtimeService deviceRealtimeService;

    @Override
    public boolean executeControl(UUID farmId, UUID deviceId, String command) {
        log.info("Processing AutoThresholdStrategy for device {}...", deviceId);

        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found"));

        if (device.getOperatingMode() != OperatingModeEnum.AUTO) {
            log.error("Refused auto execution because device {} is in MANUAL mode", deviceId);
            return false;
        }

        String adafruitFeedKey = device.getAdafruitFeedKey();
        if (adafruitFeedKey == null || adafruitFeedKey.isBlank()) {
            // BUG-01: Ném exception thay vì silent return false
            throw new IllegalStateException(
                    "Thiết bị " + deviceId + " chưa có Adafruit Feed Key. Vui lòng liên hệ Admin.");
        }

        // BUG-02: Publish MQTT trước, lưu DB sau khi thành công
        mqttSenderService.sendCommand(adafruitFeedKey, command);
        device.setIsActive("ON".equalsIgnoreCase(command));
        Device saved = deviceRepository.save(device);
        deviceRealtimeService.publishDeviceState(saved);

        log.info("Sent auto command {} (Sensor Based) to device {}", command, deviceId);
        return true;
    }
}
