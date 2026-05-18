package com.yoloFarm.api.service.mqtt.observer;

import com.yoloFarm.api.dto.SensorData;
import com.yoloFarm.api.entity.Rule;
import com.yoloFarm.api.repository.RuleRepository;
import com.yoloFarm.api.service.AutomationConfigService;
import com.yoloFarm.api.service.automation.AutomationRuntimeStateService;
import com.yoloFarm.api.service.NotificationService;
import com.yoloFarm.api.service.strategy.AutoThresholdStrategy;
import com.yoloFarm.api.service.strategy.IrrigationContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class RuleEngineObserver implements Observer {

    private final RuleRepository ruleRepository;
    private final IrrigationContext irrigationContext;
    private final AutoThresholdStrategy autoThresholdStrategy;
    private final NotificationService notificationService;
    private final AutomationRuntimeStateService automationRuntimeStateService;
    private final AutomationConfigService automationConfigService;
    private final Clock clock;

    @Override
    public void update(SensorData data) {
        log.info("RuleEngineObserver: Received sensor data: {} = {}", data.metricType(), data.value());

        // JOIN FETCH để tránh N+1 queries khi truy cập rule.getFarm() và
        // rule.getActionDevice()
        List<Rule> rules = ruleRepository.findActiveRulesWithAssociations(data.deviceId());

        for (Rule rule : rules) {
            boolean conditionMet = evaluateCondition(data.value(), rule.getOperator(), rule.getThresholdValue());

            if (!conditionMet) {
                continue;
            }

            String command = rule.getActionCommand().name();
            long cooldownSeconds = automationConfigService.getCommandCooldownSeconds();
            if (shouldSkipAsAlreadyInDesiredState(rule, command)) {
                continue;
            }

            Instant now = clock.instant();
            if (automationRuntimeStateService.isRuleCommandInCooldown(
                    rule.getId(), command, cooldownSeconds, now)) {
                log.debug("Rule {} is in cooldown for command {}", rule.getId(), command);
                if (automationRuntimeStateService.shouldNotifyRuleCooldown(
                        rule.getId(), command, cooldownSeconds, now)) {
                    String msg = String.format(
                            "Auto system: skipped repeated %s command for [%s] because rule [%s] is still in %d-second cooldown.",
                            command,
                            rule.getActionDevice().getName(),
                            rule.getRuleName(),
                            cooldownSeconds);
                    notificationService.createSystemNotification(rule.getFarm().getOwner().getId(), msg);
                }
                continue;
            }

            log.info("Rule triggered: auto control device based on sensor ({} {} {})",
                    data.value(), rule.getOperator(), rule.getThresholdValue());

            boolean success = irrigationContext.executeControl(
                    autoThresholdStrategy,
                    rule.getFarm().getId(),
                    rule.getActionDevice().getId(),
                    command);

            if (!success) {
                continue;
            }

            automationRuntimeStateService.markRuleCommandExecuted(rule.getId(), command, now);
            automationRuntimeStateService.markAutoCommand(rule.getActionDevice().getId(), command, now);

            String msg = String.format("Auto system: %s [%s] triggered by %s (%s %s %s)",
                    command,
                    rule.getActionDevice().getName(),
                    data.metricType(),
                    data.value(),
                    rule.getOperator(),
                    rule.getThresholdValue());
            notificationService.createSystemNotification(rule.getFarm().getOwner().getId(), msg);
        }
    }

    private boolean shouldSkipAsAlreadyInDesiredState(Rule rule, String command) {
        Boolean isActive = rule.getActionDevice().getIsActive();
        boolean active = Boolean.TRUE.equals(isActive);

        if ("ON".equalsIgnoreCase(command) && active) {
            log.debug("Skip rule {} - actuator {} is already ON", rule.getId(), rule.getActionDevice().getId());
            return true;
        }

        if ("OFF".equalsIgnoreCase(command) && !active) {
            log.debug("Skip rule {} - actuator {} is already OFF", rule.getId(), rule.getActionDevice().getId());
            return true;
        }

        return false;
    }

    private boolean evaluateCondition(Float presentValue, String operator, Float threshold) {
        if (presentValue == null || operator == null || threshold == null) {
            return false;
        }

        return switch (operator) {
            case ">" -> presentValue > threshold;
            case "<" -> presentValue < threshold;
            case ">=" -> presentValue >= threshold;
            case "<=" -> presentValue <= threshold;
            case "==" -> presentValue.equals(threshold);
            case "!=" -> !presentValue.equals(threshold);
            default -> false;
        };
    }
}
