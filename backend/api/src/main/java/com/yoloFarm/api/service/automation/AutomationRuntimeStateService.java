package com.yoloFarm.api.service.automation;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AutomationRuntimeStateService {

    private final ConcurrentHashMap<UUID, Instant> autoOnSinceByDevice = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Instant> lastRuleCommandByKey = new ConcurrentHashMap<>();

    public boolean isRuleCommandInCooldown(UUID ruleId, String command, long cooldownSeconds, Instant now) {
        if (ruleId == null || command == null || cooldownSeconds <= 0) {
            return false;
        }

        Instant previous = lastRuleCommandByKey.get(buildRuleCommandKey(ruleId, command));
        if (previous == null) {
            return false;
        }

        return previous.plusSeconds(cooldownSeconds).isAfter(now);
    }

    public void markRuleCommandExecuted(UUID ruleId, String command, Instant now) {
        if (ruleId == null || command == null) {
            return;
        }
        lastRuleCommandByKey.put(buildRuleCommandKey(ruleId, command), now);
    }

    public Instant getAutoOnSince(UUID deviceId) {
        if (deviceId == null) {
            return null;
        }
        return autoOnSinceByDevice.get(deviceId);
    }

    public void markAutoCommand(UUID deviceId, String command, Instant now) {
        if (deviceId == null || command == null) {
            return;
        }

        if ("ON".equalsIgnoreCase(command)) {
            autoOnSinceByDevice.put(deviceId, now);
            return;
        }

        if ("OFF".equalsIgnoreCase(command)) {
            autoOnSinceByDevice.remove(deviceId);
        }
    }

    private String buildRuleCommandKey(UUID ruleId, String command) {
        return ruleId + ":" + command.toUpperCase();
    }

    /**
     * BUG-05: Xóa toàn bộ state liên quan đến device khi device bị xóa.
     * Ngăn accumulated stale entries trong long-running production.
     */
    public void evictDeviceState(UUID deviceId) {
        if (deviceId == null) {
            return;
        }
        autoOnSinceByDevice.remove(deviceId);
    }

    /**
     * BUG-05: Xóa toàn bộ state liên quan đến rule khi rule bị xóa.
     * Cleanup cả ON và OFF cooldown keys cho rule đó.
     */
    public void evictRuleState(UUID ruleId) {
        if (ruleId == null) {
            return;
        }
        lastRuleCommandByKey.remove(buildRuleCommandKey(ruleId, "ON"));
        lastRuleCommandByKey.remove(buildRuleCommandKey(ruleId, "OFF"));
    }
}
