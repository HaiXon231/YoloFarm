package com.yoloFarm.api.repository;

import com.yoloFarm.api.entity.Rule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RuleRepository extends JpaRepository<Rule, UUID> {
    List<Rule> findByTriggerDeviceIdAndIsActiveTrue(UUID triggerDeviceId);
    List<Rule> findByFarmId(UUID farmId);
}
