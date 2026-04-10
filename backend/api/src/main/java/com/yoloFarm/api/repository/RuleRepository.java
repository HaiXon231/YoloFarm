package com.yoloFarm.api.repository;

import com.yoloFarm.api.entity.Rule;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RuleRepository extends JpaRepository<Rule, UUID> {
        List<Rule> findByTriggerDeviceIdAndIsActiveTrue(UUID triggerDeviceId);

        List<Rule> findByRuleTypeAndIsActiveTrue(com.yoloFarm.api.enums.RuleTypeEnum ruleType);

        List<Rule> findByFarmId(UUID farmId);

        List<Rule> findByFarmIdAndFarmOwnerId(UUID farmId, UUID ownerId);

        java.util.Optional<Rule> findByIdAndFarmOwnerId(UUID ruleId, UUID ownerId);

        long countByFarmId(UUID farmId);

        boolean existsByActionDeviceIdAndIsActiveTrue(UUID actionDeviceId);

        List<Rule> findByFarmIdAndActionDeviceIdAndTriggerDeviceIdAndRuleTypeAndActionCommand(
                        UUID farmId,
                        UUID actionDeviceId,
                        UUID triggerDeviceId,
                        com.yoloFarm.api.enums.RuleTypeEnum ruleType,
                        com.yoloFarm.api.enums.ActionCommandEnum actionCommand);

        List<Rule> findByFarmIdAndActionDeviceIdAndRuleTypeAndActionCommand(
                        UUID farmId,
                        UUID actionDeviceId,
                        com.yoloFarm.api.enums.RuleTypeEnum ruleType,
                        com.yoloFarm.api.enums.ActionCommandEnum actionCommand);

        /**
         * Lấy active rules kèm Farm + ActionDevice (JOIN FETCH) để tránh N+1 queries
         * Dùng cho RuleEngineObserver
         */
        @Query("SELECT r FROM Rule r JOIN FETCH r.farm JOIN FETCH r.actionDevice WHERE r.triggerDevice.id = :deviceId AND r.isActive = true")
        List<Rule> findActiveRulesWithAssociations(@Param("deviceId") UUID deviceId);

        /**
         * Lấy các rule SCHEDULE đang active kèm Farm.owner + ActionDevice để tránh
         * LazyInitializationException trong luồng @Scheduled.
         */
        @Query("SELECT r FROM Rule r " +
                        "JOIN FETCH r.farm f " +
                        "JOIN FETCH f.owner " +
                        "JOIN FETCH r.actionDevice " +
                        "WHERE r.ruleType = :ruleType AND r.isActive = true")
        List<Rule> findActiveScheduledRulesWithAssociations(
                        @Param("ruleType") com.yoloFarm.api.enums.RuleTypeEnum ruleType);

        @Modifying
        @Query("DELETE FROM Rule r WHERE r.triggerDevice.id = :deviceId OR r.actionDevice.id = :deviceId")
        int deleteRulesBoundToDevice(@Param("deviceId") UUID deviceId);

        @Query("SELECT DISTINCT r.ruleName FROM Rule r WHERE r.triggerDevice.id = :deviceId OR r.actionDevice.id = :deviceId")
        List<String> findRuleNamesBoundToDevice(@Param("deviceId") UUID deviceId);
}
