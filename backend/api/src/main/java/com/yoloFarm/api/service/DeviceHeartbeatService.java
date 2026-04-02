package com.yoloFarm.api.service;

import com.yoloFarm.api.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class DeviceHeartbeatService {

    private final DeviceRepository deviceRepository;

    /**
     * Dọn dẹp trạng thái các thiết bị mất kết nối.
     * Chạy mỗi phút tại giây thứ 30.
     */
    @Scheduled(cron = "30 * * * * *")
    @Transactional
    public void cleanupStaleConnections() {
        // Ngưỡng thời gian: 5 phút không có dữ liệu mới
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(5);
        
        log.debug("HeartbeatJanitor: Bắt đầu kiểm tra thiết bị mất kết nối (Threshold: {})", threshold);
        
        try {
            deviceRepository.markStaleDevicesAsOffline(threshold);
            log.debug("HeartbeatJanitor: Hoàn tất dọn dẹp thiết bị offline.");
        } catch (Exception e) {
            log.error("HeartbeatJanitor: Lỗi khi cập nhật trạng thái offline", e);
        }
    }
}
