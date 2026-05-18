package com.yoloFarm.api.service;

import com.yoloFarm.api.dto.request.RuleCreateRequest;
import com.yoloFarm.api.dto.response.RuleResponse;
import com.yoloFarm.api.entity.Device;
import com.yoloFarm.api.entity.Farm;
import com.yoloFarm.api.entity.Rule;
import com.yoloFarm.api.enums.RuleTypeEnum;
import com.yoloFarm.api.repository.DeviceRepository;
import com.yoloFarm.api.repository.FarmRepository;
import com.yoloFarm.api.repository.RuleRepository;
import com.yoloFarm.api.service.automation.AutomationRuntimeStateService;
import com.yoloFarm.api.enums.ActionCommandEnum;
import com.yoloFarm.api.enums.OperatingModeEnum;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RuleService {

    private static final Set<String> ALLOWED_OPERATORS = Set.of(">", "<", ">=", "<=", "==", "!=");

    private final RuleRepository ruleRepository;
    private final FarmRepository farmRepository;
    private final DeviceRepository deviceRepository;
    private final AutomationRuntimeStateService automationRuntimeStateService;

    @Transactional
    public RuleResponse createRule(RuleCreateRequest request, UUID ownerId) {
        validateRuleShape(request);

        Farm farm = farmRepository.findByIdAndOwnerId(request.getFarmId(), ownerId)
                .orElseThrow(() -> new EntityNotFoundException("Farm not found with id: " + request.getFarmId()));

        Device actionDevice = deviceRepository.findByIdAndFarmOwnerId(request.getActionDeviceId(), ownerId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Action Device not found with id: " + request.getActionDeviceId()));

        if (!actionDevice.getFarm().getId().equals(farm.getId())) {
            throw new AccessDeniedException("Action device không thuộc farm này");
        }

        Device triggerDevice = null;
        if (request.getRuleType() == com.yoloFarm.api.enums.RuleTypeEnum.CONDITION) {
            triggerDevice = deviceRepository.findByIdAndFarmOwnerId(request.getTriggerDeviceId(), ownerId)
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Trigger Device not found with id: " + request.getTriggerDeviceId()));

            if (!triggerDevice.getFarm().getId().equals(farm.getId())) {
                throw new AccessDeniedException("Trigger device không thuộc farm này");
            }
        }

        String operator = request.getRuleType() == com.yoloFarm.api.enums.RuleTypeEnum.CONDITION
                ? request.getOperator().trim()
                : null;

        Float thresholdValue = request.getRuleType() == com.yoloFarm.api.enums.RuleTypeEnum.CONDITION
                ? request.getThresholdValue()
                : null;

        String cronExpression = request.getRuleType() == com.yoloFarm.api.enums.RuleTypeEnum.SCHEDULE
                ? normalizeCronExpression(request.getCronExpression())
                : null;

        Rule rule = Rule.builder()
                .farm(farm)
                .ruleName(request.getRuleName())
                .ruleType(request.getRuleType())
                .triggerDevice(triggerDevice)
                .operator(operator)
                .thresholdValue(thresholdValue)
                .cronExpression(cronExpression)
                .actionDevice(actionDevice)
                .actionCommand(request.getActionCommand())
                .isActive(false)
                .build();

        rule = ruleRepository.save(rule);
        rule = syncPairActivationAfterUpsert(rule);
        return mapToResponse(rule);
    }

    public List<RuleResponse> getRulesByFarmId(UUID farmId, UUID ownerId) {
        if (!farmRepository.existsByIdAndOwnerId(farmId, ownerId)) {
            throw new AccessDeniedException("Bạn không có quyền truy cập rules của nông trại này");
        }

        return ruleRepository.findByFarmIdAndFarmOwnerId(farmId, ownerId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public RuleResponse toggleRule(UUID ruleId, boolean isActive, UUID ownerId) {
        Rule rule = ruleRepository.findByIdAndFarmOwnerId(ruleId, ownerId)
                .orElseThrow(() -> new EntityNotFoundException("Rule not found with id: " + ruleId));

        if (isActive) {
            Optional<Rule> complementaryOpt = findComplementaryRule(rule);
            Rule complementaryRule = complementaryOpt.orElseThrow(
                    () -> new IllegalStateException(
                            "Rule chỉ được bật khi đã có cặp ON và OFF cho cùng action device."));
            validatePairLogic(rule, complementaryRule);

            rule.setIsActive(true);
            complementaryRule.setIsActive(true);
            ruleRepository.save(complementaryRule);
        } else {
            for (Rule pairedRule : findRulesInPairGroup(rule)) {
                if (!pairedRule.getId().equals(rule.getId()) && Boolean.TRUE.equals(pairedRule.getIsActive())) {
                    pairedRule.setIsActive(false);
                    ruleRepository.save(pairedRule);
                }
            }
        }

        rule.setIsActive(isActive);
        rule = ruleRepository.save(rule);
        return mapToResponse(rule);
    }

    @Transactional
    public void deleteRule(UUID ruleId, UUID ownerId) {
        Rule rule = ruleRepository.findByIdAndFarmOwnerId(ruleId, ownerId)
                .orElseThrow(() -> new EntityNotFoundException("Rule not found with id: " + ruleId));

        Device actionDevice = rule.getActionDevice();
        boolean deletedRuleWasActive = Boolean.TRUE.equals(rule.getIsActive());
        UUID deletedRuleId = rule.getId();

        ruleRepository.delete(rule);

        // BUG-05: Cleanup cooldown state để tránh stale entries sau khi rule bị xóa
        automationRuntimeStateService.evictRuleState(deletedRuleId);

        if (actionDevice == null || actionDevice.getId() == null || !deletedRuleWasActive) {
            return;
        }

        boolean hasRemainingActiveRules = ruleRepository.existsByActionDeviceIdAndIsActiveTrue(actionDevice.getId());
        if (hasRemainingActiveRules) {
            return;
        }

        if (actionDevice.getOperatingMode() == OperatingModeEnum.MANUAL) {
            return;
        }

        actionDevice.setOperatingMode(OperatingModeEnum.MANUAL);
        deviceRepository.save(actionDevice);
        log.info("RuleService: Action device {} switched to MANUAL because the last active rule was deleted.",
                actionDevice.getId());
    }

    @Transactional
    public RuleResponse updateRule(UUID ruleId, RuleCreateRequest request, UUID ownerId) {
        validateRuleShape(request);

        Rule existing = ruleRepository.findByIdAndFarmOwnerId(ruleId, ownerId)
                .orElseThrow(() -> new EntityNotFoundException("Rule not found with id: " + ruleId));

        Farm farm = farmRepository.findByIdAndOwnerId(request.getFarmId(), ownerId)
                .orElseThrow(() -> new EntityNotFoundException("Farm not found with id: " + request.getFarmId()));

        Device actionDevice = deviceRepository.findByIdAndFarmOwnerId(request.getActionDeviceId(), ownerId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Action Device not found with id: " + request.getActionDeviceId()));

        if (!actionDevice.getFarm().getId().equals(farm.getId())) {
            throw new AccessDeniedException("Action device không thuộc farm này");
        }

        Device triggerDevice = null;
        if (request.getRuleType() == com.yoloFarm.api.enums.RuleTypeEnum.CONDITION) {
            triggerDevice = deviceRepository.findByIdAndFarmOwnerId(request.getTriggerDeviceId(), ownerId)
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Trigger Device not found with id: " + request.getTriggerDeviceId()));
            if (!triggerDevice.getFarm().getId().equals(farm.getId())) {
                throw new AccessDeniedException("Trigger device không thuộc farm này");
            }
        }

        String operator = request.getRuleType() == com.yoloFarm.api.enums.RuleTypeEnum.CONDITION
                ? request.getOperator().trim()
                : null;

        Float thresholdValue = request.getRuleType() == com.yoloFarm.api.enums.RuleTypeEnum.CONDITION
                ? request.getThresholdValue()
                : null;

        String cronExpression = request.getRuleType() == com.yoloFarm.api.enums.RuleTypeEnum.SCHEDULE
                ? normalizeCronExpression(request.getCronExpression())
                : null;

        existing.setFarm(farm);
        existing.setRuleName(request.getRuleName());
        existing.setRuleType(request.getRuleType());
        existing.setTriggerDevice(triggerDevice);
        existing.setOperator(operator);
        existing.setThresholdValue(thresholdValue);
        existing.setCronExpression(cronExpression);
        existing.setActionDevice(actionDevice);
        existing.setActionCommand(request.getActionCommand());

        existing = ruleRepository.save(existing);
        existing = syncPairActivationAfterUpsert(existing);
        return mapToResponse(existing);
    }

    private Rule syncPairActivationAfterUpsert(Rule rule) {
        Optional<Rule> complementaryOpt = findComplementaryRule(rule);
        if (complementaryOpt.isEmpty()) {
            if (Boolean.TRUE.equals(rule.getIsActive())) {
                rule.setIsActive(false);
                rule = ruleRepository.save(rule);
            }
            log.info("RuleService: Rule {} '{}' was set to inactive because it lacks an ON/OFF pair.",
                    rule.getRuleType(), rule.getRuleName());
            return rule;
        }

        Rule complementaryRule = complementaryOpt.get();
        validatePairLogic(rule, complementaryRule);

        if (!Boolean.TRUE.equals(complementaryRule.getIsActive())) {
            complementaryRule.setIsActive(true);
            ruleRepository.save(complementaryRule);
        }
        if (!Boolean.TRUE.equals(rule.getIsActive())) {
            rule.setIsActive(true);
            rule = ruleRepository.save(rule);
        }

        return rule;
    }

    private Optional<Rule> findComplementaryRule(Rule rule) {
        ActionCommandEnum complementaryCommand = complementaryCommand(rule.getActionCommand());
        List<Rule> candidates;

        if (rule.getRuleType() == RuleTypeEnum.CONDITION) {
            if (rule.getTriggerDevice() == null || rule.getTriggerDevice().getId() == null) {
                throw new IllegalStateException("Rule CONDITION thiếu trigger device để ghép cặp ON/OFF.");
            }
            candidates = ruleRepository.findByFarmIdAndActionDeviceIdAndTriggerDeviceIdAndRuleTypeAndActionCommand(
                    rule.getFarm().getId(),
                    rule.getActionDevice().getId(),
                    rule.getTriggerDevice().getId(),
                    RuleTypeEnum.CONDITION,
                    complementaryCommand);
        } else {
            candidates = ruleRepository.findByFarmIdAndActionDeviceIdAndRuleTypeAndActionCommand(
                    rule.getFarm().getId(),
                    rule.getActionDevice().getId(),
                    RuleTypeEnum.SCHEDULE,
                    complementaryCommand);
        }

        List<Rule> filtered = candidates.stream()
                .filter(candidate -> !candidate.getId().equals(rule.getId()))
                .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            return Optional.empty();
        }
        if (filtered.size() > 1) {
            throw new IllegalStateException(
                    "Có nhiều rule đối ứng ON/OFF. Vui lòng giữ một cặp rule rõ ràng cho từng luồng tự động.");
        }

        return Optional.of(filtered.get(0));
    }

    private List<Rule> findRulesInPairGroup(Rule rule) {
        if (rule.getRuleType() == RuleTypeEnum.CONDITION) {
            if (rule.getTriggerDevice() == null || rule.getTriggerDevice().getId() == null) {
                return List.of(rule);
            }
            return ruleRepository.findByFarmIdAndActionDeviceIdAndTriggerDeviceIdAndRuleType(
                    rule.getFarm().getId(),
                    rule.getActionDevice().getId(),
                    rule.getTriggerDevice().getId(),
                    RuleTypeEnum.CONDITION);
        }

        return ruleRepository.findByFarmIdAndActionDeviceIdAndRuleType(
                rule.getFarm().getId(),
                rule.getActionDevice().getId(),
                RuleTypeEnum.SCHEDULE);
    }

    private void validatePairLogic(Rule rule, Rule complementaryRule) {
        if (rule.getRuleType() == RuleTypeEnum.CONDITION) {
            validateConditionPairLogic(rule, complementaryRule);
            return;
        }
        validateSchedulePairLogic(rule, complementaryRule);
    }

    private void validateConditionPairLogic(Rule rule, Rule complementaryRule) {
        Rule onRule = rule.getActionCommand() == ActionCommandEnum.ON ? rule : complementaryRule;
        Rule offRule = rule.getActionCommand() == ActionCommandEnum.OFF ? rule : complementaryRule;

        String onOperator = normalizeOperator(onRule.getOperator());
        String offOperator = normalizeOperator(offRule.getOperator());
        Float onThreshold = onRule.getThresholdValue();
        Float offThreshold = offRule.getThresholdValue();

        if (onThreshold == null || offThreshold == null) {
            throw new IllegalStateException("Rule CONDITION thiếu threshold để ghép cặp ON/OFF hợp lệ.");
        }

        boolean onByLow = isLowOperator(onOperator);
        boolean onByHigh = isHighOperator(onOperator);
        boolean offByLow = isLowOperator(offOperator);
        boolean offByHigh = isHighOperator(offOperator);

        boolean validLowToHigh = onByLow && offByHigh && onThreshold < offThreshold;
        boolean validHighToLow = onByHigh && offByLow && offThreshold < onThreshold;

        if (!validLowToHigh && !validHighToLow) {
            throw new IllegalStateException(
                    "Cặp rule CONDITION không hợp lệ. Cần dạng hysteresis: ON(< ngưỡng thấp) + OFF(> ngưỡng cao) hoặc ngược lại.");
        }
    }

    private void validateSchedulePairLogic(Rule rule, Rule complementaryRule) {
        String cronA = normalizeCronExpression(rule.getCronExpression());
        String cronB = normalizeCronExpression(complementaryRule.getCronExpression());

        if (cronA.equals(cronB)) {
            throw new IllegalStateException(
                    "Cặp rule SCHEDULE không hợp lệ: ON và OFF không được trùng cùng một lịch chạy.");
        }
    }

    private ActionCommandEnum complementaryCommand(ActionCommandEnum actionCommand) {
        ActionCommandEnum complementary = actionCommand == ActionCommandEnum.ON
                ? ActionCommandEnum.OFF
                : ActionCommandEnum.ON;
        return complementary;
    }

    private String normalizeOperator(String operator) {
        if (operator == null) {
            return "";
        }
        return operator.trim();
    }

    private boolean isLowOperator(String operator) {
        return "<".equals(operator) || "<=".equals(operator);
    }

    private boolean isHighOperator(String operator) {
        return ">".equals(operator) || ">=".equals(operator);
    }

    private RuleResponse mapToResponse(Rule rule) {
        RuleResponse response = new RuleResponse();
        response.setId(rule.getId());
        response.setFarmId(rule.getFarm().getId());

        response.setRuleType(rule.getRuleType());
        if (rule.getTriggerDevice() != null) {
            response.setTriggerDeviceId(rule.getTriggerDevice().getId());
        }
        response.setOperator(rule.getOperator());
        response.setThresholdValue(rule.getThresholdValue());
        response.setCronExpression(rule.getCronExpression());

        if (rule.getActionDevice() != null) {
            response.setActionDeviceId(rule.getActionDevice().getId());
        }
        response.setActionCommand(rule.getActionCommand());
        response.setIsActive(rule.getIsActive());

        response.setRuleName(rule.getRuleName());
        return response;
    }

    private void validateRuleShape(RuleCreateRequest request) {
        if (request.getRuleType() == com.yoloFarm.api.enums.RuleTypeEnum.CONDITION) {
            if (request.getTriggerDeviceId() == null) {
                throw new IllegalArgumentException("Rule CONDITION bắt buộc trigger_device_id");
            }
            if (request.getOperator() == null || request.getOperator().isBlank()) {
                throw new IllegalArgumentException("Rule CONDITION bắt buộc operator");
            }
            if (!ALLOWED_OPERATORS.contains(request.getOperator().trim())) {
                throw new IllegalArgumentException("Operator không hợp lệ. Chỉ chấp nhận >, <, >=, <=, ==, !=");
            }
            if (request.getThresholdValue() == null) {
                throw new IllegalArgumentException("Rule CONDITION bắt buộc threshold_value");
            }
            if (request.getCronExpression() != null && !request.getCronExpression().isBlank()) {
                throw new IllegalArgumentException("Rule CONDITION không được truyền cron_expression");
            }
            return;
        }

        if (request.getRuleType() == com.yoloFarm.api.enums.RuleTypeEnum.SCHEDULE) {
            if (request.getCronExpression() == null || request.getCronExpression().isBlank()) {
                throw new IllegalArgumentException("Rule SCHEDULE bắt buộc cron_expression");
            }
            normalizeCronExpression(request.getCronExpression());

            if (request.getTriggerDeviceId() != null || request.getThresholdValue() != null
                    || (request.getOperator() != null && !request.getOperator().isBlank())) {
                throw new IllegalArgumentException(
                        "Rule SCHEDULE không được truyền trigger_device_id/operator/threshold_value");
            }
            return;
        }

        throw new IllegalArgumentException("Rule type không hợp lệ");
    }

    private String normalizeCronExpression(String rawCron) {
        String cron = rawCron.trim();
        String[] parts = cron.split("\\s+");
        if (parts.length == 5) {
            cron = "0 " + cron;
        }
        try {
            CronExpression.parse(cron);
            return cron;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Cron expression không hợp lệ: " + rawCron);
        }
    }
}
