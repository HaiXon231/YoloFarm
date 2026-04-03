package com.yoloFarm.api.service;

import com.yoloFarm.api.dto.request.RuleCreateRequest;
import com.yoloFarm.api.dto.response.RuleResponse;
import com.yoloFarm.api.entity.Device;
import com.yoloFarm.api.entity.Farm;
import com.yoloFarm.api.entity.Rule;
import com.yoloFarm.api.repository.DeviceRepository;
import com.yoloFarm.api.repository.FarmRepository;
import com.yoloFarm.api.repository.RuleRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RuleService {

    private static final Set<String> ALLOWED_OPERATORS = Set.of(">", "<", ">=", "<=", "==", "!=");

    private final RuleRepository ruleRepository;
    private final FarmRepository farmRepository;
    private final DeviceRepository deviceRepository;

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
                .isActive(true)
                .build();

        rule = ruleRepository.save(rule);
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

        rule.setIsActive(isActive);
        rule = ruleRepository.save(rule);
        return mapToResponse(rule);
    }

    @Transactional
    public void deleteRule(UUID ruleId, UUID ownerId) {
        Rule rule = ruleRepository.findByIdAndFarmOwnerId(ruleId, ownerId)
                .orElseThrow(() -> new EntityNotFoundException("Rule not found with id: " + ruleId));
        ruleRepository.delete(rule);
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

        return mapToResponse(ruleRepository.save(existing));
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
