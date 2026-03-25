package com.yoloFarm.api.repository;

import com.yoloFarm.api.entity.Rule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RuleRepository extends JpaRepository<Rule, UUID> {
    List<Rule> findByTriggerDeviceIdAndIsActiveTrue(UUID triggerDeviceId);
    List<Rule> findByFarmId(UUID farmId);

    /**
     * Lấy active rules kèm Farm + ActionDevice (JOIN FETCH) để tránh N+1 queries
     * Dùng cho RuleEngineObserver
     */
    @Query("SELECT r FROM Rule r JOIN FETCH r.farm JOIN FETCH r.actionDevice WHERE r.triggerDevice.id = :deviceId AND r.isActive = true")
    List<Rule> findActiveRulesWithAssociations(@Param("deviceId") UUID deviceId);
}
