package com.yoloFarm.api;

import com.yoloFarm.api.entity.Device;
import com.yoloFarm.api.entity.Farm;
import com.yoloFarm.api.entity.Rule;
import com.yoloFarm.api.enums.ActionCommandEnum;
import com.yoloFarm.api.enums.OperatingModeEnum;
import com.yoloFarm.api.enums.RuleTypeEnum;
import com.yoloFarm.api.repository.DeviceRepository;
import com.yoloFarm.api.repository.FarmRepository;
import com.yoloFarm.api.repository.RuleRepository;
import com.yoloFarm.api.service.RuleService;
import com.yoloFarm.api.service.automation.AutomationRuntimeStateService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RuleServiceDeleteLifecycleTest {

    @Mock
    private RuleRepository ruleRepository;

    @Mock
    private FarmRepository farmRepository;

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private AutomationRuntimeStateService automationRuntimeStateService;

    @InjectMocks
    private RuleService ruleService;

    @Test
    void shouldDeleteComplementaryRule_whenDeletingRuleInPair() {
        UUID ownerId = UUID.randomUUID();
        UUID farmId = UUID.randomUUID();
        UUID actionDeviceId = UUID.randomUUID();
        UUID triggerDeviceId = UUID.randomUUID();
        UUID onRuleId = UUID.randomUUID();
        UUID offRuleId = UUID.randomUUID();

        Farm farm = new Farm();
        farm.setId(farmId);

        Device actionDevice = new Device();
        actionDevice.setId(actionDeviceId);
        actionDevice.setOperatingMode(OperatingModeEnum.AUTO);

        Device triggerDevice = new Device();
        triggerDevice.setId(triggerDeviceId);

        Rule onRule = new Rule();
        onRule.setId(onRuleId);
        onRule.setFarm(farm);
        onRule.setActionDevice(actionDevice);
        onRule.setTriggerDevice(triggerDevice);
        onRule.setRuleType(RuleTypeEnum.CONDITION);
        onRule.setActionCommand(ActionCommandEnum.ON);
        onRule.setIsActive(true);

        Rule offRule = new Rule();
        offRule.setId(offRuleId);
        offRule.setFarm(farm);
        offRule.setActionDevice(actionDevice);
        offRule.setTriggerDevice(triggerDevice);
        offRule.setRuleType(RuleTypeEnum.CONDITION);
        offRule.setActionCommand(ActionCommandEnum.OFF);
        offRule.setIsActive(true);

        when(ruleRepository.findByIdAndFarmOwnerId(onRuleId, ownerId)).thenReturn(Optional.of(onRule));
        when(ruleRepository.findByFarmIdAndActionDeviceIdAndTriggerDeviceIdAndRuleTypeAndActionCommand(
                farmId,
                actionDeviceId,
                triggerDeviceId,
                RuleTypeEnum.CONDITION,
                ActionCommandEnum.OFF))
                .thenReturn(List.of(offRule));
        when(ruleRepository.existsByActionDeviceIdAndIsActiveTrue(actionDeviceId)).thenReturn(false);

        ruleService.deleteRule(onRuleId, ownerId);

        verify(ruleRepository).delete(onRule);
        verify(ruleRepository).delete(offRule);
        verify(automationRuntimeStateService).evictRuleState(onRuleId);
        verify(automationRuntimeStateService).evictRuleState(offRuleId);
        verify(deviceRepository).save(actionDevice);
    }

    @Test
    void shouldForceActionDeviceToManual_whenDeletingLastActiveRule() {
        UUID ownerId = UUID.randomUUID();
        UUID ruleId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();

        Device actionDevice = new Device();
        actionDevice.setId(deviceId);
        actionDevice.setOperatingMode(OperatingModeEnum.AUTO);

        Rule rule = new Rule();
        rule.setId(ruleId);
        rule.setActionDevice(actionDevice);
        rule.setIsActive(true);

        when(ruleRepository.findByIdAndFarmOwnerId(ruleId, ownerId)).thenReturn(Optional.of(rule));
        when(ruleRepository.existsByActionDeviceIdAndIsActiveTrue(deviceId)).thenReturn(false);

        ruleService.deleteRule(ruleId, ownerId);

        verify(ruleRepository).delete(rule);
        verify(deviceRepository).save(actionDevice);
    }

    @Test
    void shouldKeepAutoMode_whenOtherActiveRulesStillExist() {
        UUID ownerId = UUID.randomUUID();
        UUID ruleId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();

        Device actionDevice = new Device();
        actionDevice.setId(deviceId);
        actionDevice.setOperatingMode(OperatingModeEnum.AUTO);

        Rule rule = new Rule();
        rule.setId(ruleId);
        rule.setActionDevice(actionDevice);
        rule.setIsActive(true);

        when(ruleRepository.findByIdAndFarmOwnerId(ruleId, ownerId)).thenReturn(Optional.of(rule));
        when(ruleRepository.existsByActionDeviceIdAndIsActiveTrue(deviceId)).thenReturn(true);

        ruleService.deleteRule(ruleId, ownerId);

        verify(ruleRepository).delete(rule);
        verify(deviceRepository, never()).save(actionDevice);
    }

    @Test
    void shouldSkipManualFallback_whenDeletedRuleIsInactive() {
        UUID ownerId = UUID.randomUUID();
        UUID ruleId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();

        Device actionDevice = new Device();
        actionDevice.setId(deviceId);
        actionDevice.setOperatingMode(OperatingModeEnum.AUTO);

        Rule rule = new Rule();
        rule.setId(ruleId);
        rule.setActionDevice(actionDevice);
        rule.setIsActive(false);

        when(ruleRepository.findByIdAndFarmOwnerId(ruleId, ownerId)).thenReturn(Optional.of(rule));

        ruleService.deleteRule(ruleId, ownerId);

        verify(ruleRepository).delete(rule);
        verify(ruleRepository, never()).existsByActionDeviceIdAndIsActiveTrue(deviceId);
        verify(deviceRepository, never()).save(actionDevice);
    }

    @Test
    void shouldThrowWhenDeletingRule_thatDoesNotExist() {
        UUID ownerId = UUID.randomUUID();
        UUID ruleId = UUID.randomUUID();

        when(ruleRepository.findByIdAndFarmOwnerId(ruleId, ownerId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> ruleService.deleteRule(ruleId, ownerId));
    }
}
