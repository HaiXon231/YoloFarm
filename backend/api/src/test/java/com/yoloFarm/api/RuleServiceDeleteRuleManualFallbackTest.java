package com.yoloFarm.api;

import com.yoloFarm.api.entity.Device;
import com.yoloFarm.api.entity.Rule;
import com.yoloFarm.api.enums.OperatingModeEnum;
import com.yoloFarm.api.repository.DeviceRepository;
import com.yoloFarm.api.repository.FarmRepository;
import com.yoloFarm.api.repository.RuleRepository;
import com.yoloFarm.api.service.RuleService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RuleServiceDeleteRuleManualFallbackTest {

    @Mock
    private RuleRepository ruleRepository;

    @Mock
    private FarmRepository farmRepository;

    @Mock
    private DeviceRepository deviceRepository;

    @InjectMocks
    private RuleService ruleService;

    @Test
    void deleteLastActiveRuleShouldForceActionDeviceToManual() {
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
    void deleteRuleShouldNotForceManualWhenOtherActiveRulesStillExist() {
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
    void deleteInactiveRuleShouldNotForceManual() {
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
    void deleteRuleShouldThrowWhenRuleNotFound() {
        UUID ownerId = UUID.randomUUID();
        UUID ruleId = UUID.randomUUID();

        when(ruleRepository.findByIdAndFarmOwnerId(ruleId, ownerId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> ruleService.deleteRule(ruleId, ownerId));
    }
}
