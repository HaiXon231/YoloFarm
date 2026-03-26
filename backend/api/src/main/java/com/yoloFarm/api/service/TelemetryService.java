package com.yoloFarm.api.service;

import com.yoloFarm.api.dto.response.TelemetryDataPoint;
import com.yoloFarm.api.entity.TelemetryData;
import com.yoloFarm.api.repository.DeviceRepository;
import com.yoloFarm.api.repository.TelemetryDataRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TelemetryService {

	private final TelemetryDataRepository telemetryDataRepository;
	private final DeviceRepository deviceRepository;

	public List<TelemetryDataPoint> getTelemetry(UUID ownerId, UUID deviceId, LocalDateTime start, LocalDateTime end, String aggregate) {
		if (start == null || end == null || start.isAfter(end)) {
			throw new IllegalStateException("Khoảng thời gian không hợp lệ");
		}

		if (deviceRepository.findByIdAndFarmOwnerId(deviceId, ownerId).isEmpty()) {
			if (deviceRepository.findById(deviceId).isEmpty()) {
				throw new EntityNotFoundException("Device not found with id: " + deviceId);
			}
			throw new AccessDeniedException("Bạn không có quyền truy cập telemetry của thiết bị này");
		}

		List<TelemetryData> rows = telemetryDataRepository.findByDeviceIdAndCreatedAtBetweenOrderByCreatedAtAsc(deviceId, start, end);
		if (aggregate == null || aggregate.isBlank()) {
			return rows.stream().map(this::mapPoint).toList();
		}

		return aggregateRows(rows, aggregate);
	}

	private List<TelemetryDataPoint> aggregateRows(List<TelemetryData> rows, String aggregate) {
		Map<LocalDateTime, List<Float>> buckets = new LinkedHashMap<>();
		for (TelemetryData row : rows) {
			LocalDateTime bucket = toBucket(row.getCreatedAt(), aggregate);
			buckets.computeIfAbsent(bucket, k -> new ArrayList<>()).add(row.getValue());
		}

		List<TelemetryDataPoint> result = new ArrayList<>();
		for (Map.Entry<LocalDateTime, List<Float>> entry : buckets.entrySet()) {
			float avg = (float) entry.getValue().stream().mapToDouble(Float::doubleValue).average().orElse(0.0);
			TelemetryDataPoint p = new TelemetryDataPoint();
			p.setTime(entry.getKey());
			p.setValue(avg);
			result.add(p);
		}
		result.sort(Comparator.comparing(TelemetryDataPoint::getTime));
		return result;
	}

	private LocalDateTime toBucket(LocalDateTime time, String aggregate) {
		return switch (aggregate) {
			case "15m" -> time.truncatedTo(ChronoUnit.HOURS).plusMinutes((time.getMinute() / 15) * 15L);
			case "1h" -> time.truncatedTo(ChronoUnit.HOURS);
			case "1d" -> time.toLocalDate().atStartOfDay();
			default -> throw new IllegalStateException("Giá trị aggregate không hợp lệ: " + aggregate);
		};
	}

	private TelemetryDataPoint mapPoint(TelemetryData row) {
		TelemetryDataPoint point = new TelemetryDataPoint();
		point.setTime(row.getCreatedAt());
		point.setValue(row.getValue());
		return point;
	}
}
