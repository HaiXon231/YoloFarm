package com.yoloFarm.api.service;

import com.yoloFarm.api.entity.Device;
import com.yoloFarm.api.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class DeviceHeartbeatService {

    private final DeviceRepository deviceRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Dọn dẹp trạng thái các thiết bị mất kết nối.
     * Chạy mỗi phút tại giây thứ 30.
     * Sau khi cập nhật DB, push WebSocket event để Frontend phản ứng tức thì.
     */
    @Scheduled(cron = "30 * * * * *")
    @Transactional
    public void cleanupStaleConnections() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(5);

        log.debug("HeartbeatJanitor: Bắt đầu kiểm tra thiết bị mất kết nối (Threshold: {})", threshold);

        try {
            // Bước 1: Tìm danh sách thiết bị SẮP bị offline trước khi UPDATE
            List<Device> staleDevices = deviceRepository.findStaleOnlineDevices(threshold);

            if (staleDevices.isEmpty()) {
                log.debug("HeartbeatJanitor: Không có thiết bị nào cần đánh offline.");
                return;
            }

            // Gom nhóm theo farmId để push 1 event/farm thay vì 1 event/device
            Map<UUID, List<Device>> byFarm = staleDevices.stream()
                    .collect(Collectors.groupingBy(d -> d.getFarm().getId()));

            // Bước 2: UPDATE trạng thái trong DB
            deviceRepository.markStaleDevicesAsOffline(threshold);
            log.info("HeartbeatJanitor: Đánh OFFLINE {} thiết bị thuộc {} nông trại.",
                    staleDevices.size(), byFarm.size());

            // Bước 3: Push WebSocket event cho từng farm liên quan
            byFarm.forEach((farmId, devices) -> {
                List<Map<String, Object>> offlinePayload = devices.stream()
                        .map(d -> Map.<String, Object>of(
                                "deviceId", d.getId().toString(),
                                "connectionStatus", "OFFLINE"
                        ))
                        .collect(Collectors.toList());

                messagingTemplate.convertAndSend(
                        "/topic/farm/" + farmId + "/device-status",
                        offlinePayload
                );
                log.debug("HeartbeatJanitor: Đã push offline event cho farm {}: {} thiết bị.",
                        farmId, devices.size());
            });

            // Push thêm event cho Admin Dashboard
            messagingTemplate.convertAndSend("/topic/admin/stats-changed", (Object) Map.of("reason", "devices_offline"));

        } catch (Exception e) {
            log.error("HeartbeatJanitor: Lỗi khi cập nhật trạng thái offline", e);
        }
    }
}
