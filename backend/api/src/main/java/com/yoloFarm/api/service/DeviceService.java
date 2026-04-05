package com.yoloFarm.api.service;

import com.yoloFarm.api.dto.request.DeviceRequest;
import com.yoloFarm.api.dto.response.DeviceResponse;
import com.yoloFarm.api.entity.Device;
import com.yoloFarm.api.entity.DeviceModel;
import com.yoloFarm.api.entity.Farm;
import com.yoloFarm.api.enums.ConnectionStatusEnum;
import com.yoloFarm.api.enums.DeviceStatusEnum;
import com.yoloFarm.api.enums.OperatingModeEnum;
import com.yoloFarm.api.exception.ConflictException;
import com.yoloFarm.api.repository.DeviceModelRepository;
import com.yoloFarm.api.repository.DeviceRepository;
import com.yoloFarm.api.repository.FarmRepository;
import com.yoloFarm.api.repository.RuleRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeviceService {

    private static final Pattern ADAFRUIT_FEED_KEY_PATTERN = Pattern.compile("^[a-z0-9-]{1,64}$");

    private final DeviceRepository deviceRepository;
    private final FarmRepository farmRepository;
    private final DeviceModelRepository deviceModelRepository;
    private final RuleRepository ruleRepository;
    private final NotificationService notificationService;
    private final AdafruitApiService adafruitApiService;

    @Transactional(readOnly = true)
    public List<DeviceResponse> getDevicesByFarmId(UUID farmId, UUID ownerId) {
        if (!farmRepository.existsByIdAndOwnerId(farmId, ownerId)) {
            throw new AccessDeniedException("Bạn không có quyền truy cập danh sách thiết bị của nông trại này");
        }

        return deviceRepository.findByFarmIdAndFarmOwnerId(farmId, ownerId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public DeviceResponse addDevice(DeviceRequest request, UUID ownerId) {
        Farm farm = farmRepository.findByIdAndOwnerId(request.getFarmId(), ownerId)
                .orElseThrow(() -> new EntityNotFoundException("Farm not found with id: " + request.getFarmId()));

        DeviceModel model = deviceModelRepository.findById(request.getModelId())
                .orElseThrow(
                        () -> new EntityNotFoundException("DeviceModel not found with id: " + request.getModelId()));

        Device device = Device.builder()
                .farm(farm)
                .model(model)
                .name(request.getName())
                .status(DeviceStatusEnum.PENDING)
                .connectionStatus(ConnectionStatusEnum.OFFLINE)
                .operatingMode(OperatingModeEnum.MANUAL)
                .build();

        device = deviceRepository.save(device);
        return mapToResponse(device);
    }

    @Transactional
    public DeviceResponse updateDeviceName(UUID ownerId, UUID deviceId, String newName) {
        Device device = deviceRepository.findByIdAndFarmOwnerId(deviceId, ownerId)
                .orElseThrow(() -> new EntityNotFoundException("Device not found with id: " + deviceId));

        if (newName != null && !newName.isBlank()) {
            String normalizedName = newName.trim();
            if (!normalizedName.equals(device.getName())) {
                if (device.getAdafruitFeedKey() != null && !device.getAdafruitFeedKey().isBlank()) {
                    adafruitApiService.updateFeedName(device.getAdafruitFeedKey(), normalizedName);
                }
                device.setName(normalizedName);
            }
        }

        device = deviceRepository.save(device);
        return mapToResponse(device);
    }

    @Transactional(readOnly = true)
    public void assertDeviceOwnership(UUID ownerId, UUID deviceId) {
        if (deviceRepository.findByIdAndFarmOwnerId(deviceId, ownerId).isEmpty()) {
            throw new AccessDeniedException("Bạn không có quyền thao tác thiết bị này");
        }
    }

    @Transactional
    public DeviceResponse requestDeviceRemoval(UUID ownerId, UUID deviceId) {
        Device device = deviceRepository.findByIdAndFarmOwnerId(deviceId, ownerId)
                .orElseThrow(() -> new EntityNotFoundException("Device not found with id: " + deviceId));

        if (device.getStatus() != DeviceStatusEnum.ACTIVE) {
            throw new ConflictException("Chỉ thiết bị đang ACTIVE mới có thể gửi yêu cầu gỡ bỏ");
        }

        device.setStatus(DeviceStatusEnum.PENDING_REMOVAL);
        return mapToResponse(deviceRepository.save(device));
    }

    @Transactional
    public DeviceResponse changeMode(UUID ownerId, UUID deviceId, OperatingModeEnum mode) {
        Device device = deviceRepository.findByIdAndFarmOwnerId(deviceId, ownerId)
                .orElseThrow(() -> new EntityNotFoundException("Device not found with id: " + deviceId));

        if (device.getStatus() != DeviceStatusEnum.ACTIVE) {
            throw new ConflictException("Không thể đổi chế độ khi thiết bị chưa ở trạng thái ACTIVE");
        }

        if (mode == OperatingModeEnum.AUTO) {
            boolean hasActiveRules = ruleRepository.existsByActionDeviceIdAndIsActiveTrue(deviceId);
            if (!hasActiveRules) {
                throw new IllegalStateException(
                        "Không thể bật chế độ AUTO: Thiết bị này hiện chưa có luật (Rule) nào đang hoạt động.");
            }
        }

        device.setOperatingMode(mode);
        return mapToResponse(deviceRepository.save(device));
    }

    @Transactional(readOnly = true)
    public List<DeviceResponse> getDeviceRequests(String status) {
        List<Device> devices;
        if (status == null || status.isBlank()) {
            devices = deviceRepository
                    .findByStatusIn(List.of(DeviceStatusEnum.PENDING, DeviceStatusEnum.PENDING_REMOVAL));
        } else {
            devices = deviceRepository.findByStatus(DeviceStatusEnum.valueOf(status));
        }
        return devices.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional
    public DeviceResponse approveDevice(UUID deviceId, String adafruitFeedKey) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new EntityNotFoundException("Device not found with id: " + deviceId));

        if (device.getStatus() == DeviceStatusEnum.PENDING_REMOVAL) {
            return approveDeviceRemoval(device);
        }

        ensureModerationTransitionAllowed(device, DeviceStatusEnum.PENDING, "duyệt kích hoạt");

        String resolvedFeedKeyRaw = (adafruitFeedKey == null || adafruitFeedKey.isBlank())
                ? generateAutoFeedKey(device)
                : normalizeFeedKey(adafruitFeedKey);
        String resolvedFeedKey = normalizeFeedKey(resolvedFeedKeyRaw);

        assertFeedKeyAvailable(device.getId(), resolvedFeedKey);

        // Khởi tạo Feed thực tế trên thư viện Adafruit IO thông qua REST API
        adafruitApiService.createFeed(resolvedFeedKey, device.getName());

        device.setAdafruitFeedKey(resolvedFeedKey);
        device.setStatus(DeviceStatusEnum.ACTIVE);
        Device saved = deviceRepository.save(device);

        UUID ownerId = saved.getFarm().getOwner().getId();
        notificationService.createSystemNotification(ownerId,
                "Thiết bị [" + saved.getName() + "] đã được duyệt và kích hoạt.");
        return mapToResponse(saved);
    }

    @Transactional
    public DeviceResponse rejectDevice(UUID deviceId, String rejectReason) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new EntityNotFoundException("Device not found with id: " + deviceId));

        String suffix = (rejectReason == null || rejectReason.isBlank()) ? "" : " Lý do: " + rejectReason;

        if (device.getStatus() == DeviceStatusEnum.PENDING_REMOVAL) {
            device.setStatus(DeviceStatusEnum.ACTIVE);
            Device saved = deviceRepository.save(device);

            UUID ownerId = saved.getFarm().getOwner().getId();
            notificationService.createSystemNotification(ownerId,
                    "Yêu cầu gỡ bỏ thiết bị [" + saved.getName() + "] đã bị từ chối." + suffix);
            return mapToResponse(saved);
        }

        ensureModerationTransitionAllowed(device, DeviceStatusEnum.PENDING, "từ chối yêu cầu thiết bị");

        device.setStatus(DeviceStatusEnum.REJECTED);
        Device saved = deviceRepository.save(device);

        UUID ownerId = saved.getFarm().getOwner().getId();
        notificationService.createSystemNotification(ownerId,
                "Yêu cầu thiết bị [" + saved.getName() + "] đã bị từ chối." + suffix);
        return mapToResponse(saved);
    }

    @Transactional
    public void deleteDevice(UUID deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new EntityNotFoundException("Device not found with id: " + deviceId));
        deleteDeviceAndCleanup(device, true);
    }

    private DeviceResponse approveDeviceRemoval(Device device) {
        ensureModerationTransitionAllowed(device, DeviceStatusEnum.PENDING_REMOVAL, "duyệt thu hồi thiết bị");

        UUID ownerId = device.getFarm().getOwner().getId();
        String deviceName = device.getName();

        DeviceResponse response = mapToResponse(device);
        List<String> removedRuleNames = deleteDeviceAndCleanup(device, true);

        notificationService.createSystemNotification(ownerId,
                "Yêu cầu gỡ bỏ thiết bị [" + deviceName + "] đã được duyệt. Thiết bị đã được thu hồi khỏi hệ thống.");

        if (!removedRuleNames.isEmpty()) {
            String joinedRuleNames = removedRuleNames.stream().limit(5).collect(Collectors.joining(", "));
            String suffix = removedRuleNames.size() > 5 ? " ..." : "";
            notificationService.createSystemNotification(ownerId,
                    "Các rule liên quan đến thiết bị [" + deviceName + "] đã bị xóa theo: "
                            + joinedRuleNames + suffix + ".");
        }

        return response;
    }

    private List<String> deleteDeviceAndCleanup(Device device, boolean deleteAdafruitFeed) {
        List<String> removedRuleNames = new java.util.ArrayList<>(
                new LinkedHashSet<>(ruleRepository.findRuleNamesBoundToDevice(device.getId())));

        if (deleteAdafruitFeed && device.getAdafruitFeedKey() != null && !device.getAdafruitFeedKey().isBlank()) {
            adafruitApiService.deleteFeed(device.getAdafruitFeedKey());
        }

        ruleRepository.deleteRulesBoundToDevice(device.getId());
        deviceRepository.delete(device);
        return removedRuleNames;
    }

    private String normalizeFeedKey(String rawFeedKey) {
        if (rawFeedKey == null) {
            throw new IllegalArgumentException("Feed key không được để trống");
        }

        String cleaned = rawFeedKey.trim();
        if (cleaned.isEmpty()) {
            throw new IllegalArgumentException("Feed key không được để trống");
        }

        String marker = "/feeds/";
        int markerIndex = cleaned.toLowerCase(Locale.ROOT).indexOf(marker);
        if (markerIndex >= 0) {
            cleaned = cleaned.substring(markerIndex + marker.length());
        }

        cleaned = cleaned.trim();
        if (cleaned.endsWith("/")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1).trim();
        }

        if (cleaned.isEmpty()) {
            throw new IllegalArgumentException("Feed key không được để trống");
        }

        if (!ADAFRUIT_FEED_KEY_PATTERN.matcher(cleaned).matches()) {
            throw new IllegalArgumentException(
                    "Feed key phải đúng chuẩn Adafruit: chỉ gồm a-z, 0-9, dấu '-', độ dài 1-64 ký tự, không khoảng trắng/ký tự có dấu");
        }

        return cleaned;
    }

    private void assertFeedKeyAvailable(UUID currentDeviceId, String feedKey) {
        if (!deviceRepository.existsByAdafruitFeedKeyIgnoreCase(feedKey)) {
            return;
        }

        UUID existingDeviceId = deviceRepository.findFirstByAdafruitFeedKeyIgnoreCase(feedKey)
                .map(Device::getId)
                .orElse(null);

        if (existingDeviceId != null && !existingDeviceId.equals(currentDeviceId)) {
            throw new ConflictException("Feed key đã tồn tại trong hệ thống. Vui lòng dùng mã khác");
        }
    }

    private void ensureModerationTransitionAllowed(Device device, DeviceStatusEnum expectedStatus, String action) {
        if (device.getStatus() != expectedStatus) {
            throw new ConflictException(
                    "Không thể " + action + " khi thiết bị đang ở trạng thái " + device.getStatus());
        }
    }

    private String generateAutoFeedKey(Device device) {
        String deviceSegment = device.getId().toString().replace("-", "").substring(0, 12);
        String ownerSegment = device.getFarm().getOwner().getId().toString().replace("-", "").substring(0, 8);
        String metricSegment = device.getModel().getMetricType().name().toLowerCase();
        return "u" + ownerSegment + "-d" + deviceSegment + "-" + metricSegment;
    }

    private DeviceResponse mapToResponse(Device device) {
        DeviceResponse response = new DeviceResponse();
        response.setId(device.getId());
        response.setFarmId(device.getFarm().getId());
        response.setModelId(device.getModel().getId());
        response.setName(device.getName());
        response.setStatus(device.getStatus());
        response.setAdafruitFeedKey(device.getAdafruitFeedKey());
        response.setConnectionStatus(device.getConnectionStatus());
        response.setLastSeen(device.getLastSeen());
        response.setOperatingMode(device.getOperatingMode());
        response.setIsActive(device.getIsActive());
        return response;
    }
}
