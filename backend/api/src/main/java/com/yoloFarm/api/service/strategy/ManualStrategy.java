package com.yoloFarm.api.service.strategy;

import com.yoloFarm.api.entity.Device;
import com.yoloFarm.api.enums.OperatingModeEnum;
import com.yoloFarm.api.repository.DeviceRepository;
import com.yoloFarm.api.service.mqtt.MqttSenderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class ManualStrategy implements IrrigationStrategy {

    private final DeviceRepository deviceRepository;
    private final MqttSenderService mqttSenderService;

    @Override
    public boolean executeControl(UUID farmId, UUID deviceId, String command) {
        log.info("Đang xử lý ManualStrategy cho thiết bị {}...", deviceId);

        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found"));

        if (device.getOperatingMode() != OperatingModeEnum.MANUAL) {
            log.error("Từ chối lệnh điều khiển thủ công vì thiết bị {} đang chạy chế độ TỰ ĐỘNG", deviceId);
            return false;
        }

        String adafruitFeedKey = device.getAdafruitFeedKey();
        if (adafruitFeedKey == null || adafruitFeedKey.isBlank()) {
            // BUG-01: Ném exception thay vì silent return false — caller biết lệnh thất bại
            throw new IllegalStateException(
                    "Thiết bị " + deviceId + " chưa có Adafruit Feed Key. Vui lòng liên hệ Admin.");
        }

        // BUG-02: Publish MQTT trước, chỉ lưu DB sau khi publish thành công.
        // Nếu MQTT ném exception → DB không bị cập nhật → tránh state mismatch.
        mqttSenderService.sendCommand(adafruitFeedKey, command);

        device.setIsActive("ON".equalsIgnoreCase(command));
        deviceRepository.save(device);

        log.info("Đã gửi lệnh {} thành công bằng chế độ MANUAL cho thiết bị {}", command, deviceId);
        return true;
    }
}
