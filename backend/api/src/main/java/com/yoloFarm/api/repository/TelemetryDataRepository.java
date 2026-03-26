package com.yoloFarm.api.repository;

import com.yoloFarm.api.entity.TelemetryData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TelemetryDataRepository extends JpaRepository<TelemetryData, UUID> {
	List<TelemetryData> findByDeviceIdAndCreatedAtBetweenOrderByCreatedAtAsc(UUID deviceId, LocalDateTime start, LocalDateTime end);
}
