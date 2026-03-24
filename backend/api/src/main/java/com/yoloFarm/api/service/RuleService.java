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
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RuleService {

    private final RuleRepository ruleRepository;
    private final FarmRepository farmRepository;
    private final DeviceRepository deviceRepository;

    public RuleResponse createRule(RuleCreateRequest request) {
        Farm farm = farmRepository.findById(request.getFarmId())
                .orElseThrow(() -> new EntityNotFoundException("Farm not found with id: " + request.getFarmId()));

        Device actionDevice = deviceRepository.findById(request.getActionDeviceId())
                .orElseThrow(() -> new EntityNotFoundException("Action Device not found with id: " + request.getActionDeviceId()));

        Device triggerDevice = null;
        if (request.getTriggerDeviceId() != null) {
            triggerDevice = deviceRepository.findById(request.getTriggerDeviceId())
                    .orElseThrow(() -> new EntityNotFoundException("Trigger Device not found with id: " + request.getTriggerDeviceId()));
        }

        Rule rule = Rule.builder()
                .farm(farm)
                .ruleType(request.getRuleType())
                .triggerDevice(triggerDevice)
                .operator(request.getOperator())
                .thresholdValue(request.getThresholdValue())
                .cronExpression(request.getCronExpression())
                .actionDevice(actionDevice)
                .actionCommand(request.getActionCommand())
                .isActive(true) // Mặc định bật khi mới tạo
                .build();
        
        rule = ruleRepository.save(rule);
        return mapToResponse(rule);
    }

    public List<RuleResponse> getRulesByFarmId(UUID farmId) {
        return ruleRepository.findByFarmId(farmId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public RuleResponse toggleRule(UUID ruleId, boolean isActive) {
        Rule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new EntityNotFoundException("Rule not found with id: " + ruleId));
        
        rule.setIsActive(isActive);
        rule = ruleRepository.save(rule);
        return mapToResponse(rule);
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
        
        // Entity không có cấu trúc Name lưu vào DB, dùng Tên gộp chung
        response.setRuleName("Rule-" + rule.getId());
        return response;
    }
}
