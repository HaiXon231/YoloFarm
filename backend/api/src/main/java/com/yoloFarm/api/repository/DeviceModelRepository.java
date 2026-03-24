package com.yoloFarm.api.repository;

import com.yoloFarm.api.entity.DeviceModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DeviceModelRepository extends JpaRepository<DeviceModel, UUID> {
}
