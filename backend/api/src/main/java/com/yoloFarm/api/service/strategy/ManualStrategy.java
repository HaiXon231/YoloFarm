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

        // Bước 2a: Tìm thiết bị, nén Exception nếu không có
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found"));

        // Bước 2b: Kiểm tra Operating Mode có phải MANUAL không
        if (device.getOperatingMode() != OperatingModeEnum.MANUAL) {
            log.error("Từ chối lệnh điều khiển thủ công vì thiết bị {} đang chạy chế độ TỰ ĐỘNG", deviceId);
            return false;
        }

        // Bước 2c: Móc Adafruit Feed Key và gửi lệnh
        String adafruitFeedKey = device.getAdafruitFeedKey();
        if (adafruitFeedKey == null || adafruitFeedKey.isBlank()) {
            log.warn("Thiết bị {} không có Adafruit Feed Key được cấu hình!", deviceId);
            return false;
        }

        mqttSenderService.sendCommand(adafruitFeedKey, command);

        // Cập nhật trạng thái isActive vào Database để UI đồng bộ
        device.setIsActive("ON".equalsIgnoreCase(command));
        deviceRepository.save(device);

        // Bước 2d: Xác nhận thành công
        log.info("Đã gửi lệnh {} thành công bằng chế độ MANUAL cho thiết bị {}", command, deviceId);
        return true;
    }
}
