package com.yoloFarm.api.repository;

import com.yoloFarm.api.entity.Device;
import com.yoloFarm.api.enums.DeviceStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DeviceRepository extends JpaRepository<Device, UUID> {
    List<Device> findByFarmId(UUID farmId);
    List<Device> findByFarmIdAndFarmOwnerId(UUID farmId, UUID ownerId);
    java.util.Optional<Device> findByIdAndFarmOwnerId(UUID deviceId, UUID ownerId);
    long countByFarmId(UUID farmId);
    List<Device> findByStatus(DeviceStatusEnum status);
    long countByStatus(DeviceStatusEnum status);
    long countByConnectionStatus(com.yoloFarm.api.enums.ConnectionStatusEnum connectionStatus);
    java.util.Optional<Device> findByAdafruitFeedKey(String adafruitFeedKey);

    /**
     * Lấy Device kèm Model + Farm (JOIN FETCH) để tránh LazyInitializationException
     * Dùng cho MQTT callback (ngoài Transactional context)
     */
    @Query("SELECT d FROM Device d JOIN FETCH d.model JOIN FETCH d.farm WHERE d.adafruitFeedKey = :feedKey")
    java.util.Optional<Device> findByAdafruitFeedKeyWithModelAndFarm(@Param("feedKey") String feedKey);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE Device d SET d.connectionStatus = com.yoloFarm.api.enums.ConnectionStatusEnum.OFFLINE WHERE d.connectionStatus = com.yoloFarm.api.enums.ConnectionStatusEnum.ONLINE AND (d.lastSeen < :threshold OR d.lastSeen IS NULL)")
    void markStaleDevicesAsOffline(@Param("threshold") java.time.LocalDateTime threshold);
}
