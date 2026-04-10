package com.yoloFarm.api;

import com.yoloFarm.api.dto.request.RuleCreateRequest;
import com.yoloFarm.api.dto.response.RuleResponse;
import com.yoloFarm.api.entity.Device;
import com.yoloFarm.api.entity.DeviceModel;
import com.yoloFarm.api.entity.Farm;
import com.yoloFarm.api.entity.Rule;
import com.yoloFarm.api.entity.User;
import com.yoloFarm.api.enums.ActionCommandEnum;
import com.yoloFarm.api.enums.DeviceTypeEnum;
import com.yoloFarm.api.enums.MetricTypeEnum;
import com.yoloFarm.api.enums.RoleEnum;
import com.yoloFarm.api.enums.RuleTypeEnum;
import com.yoloFarm.api.repository.DeviceRepository;
import com.yoloFarm.api.repository.FarmRepository;
import com.yoloFarm.api.repository.RuleRepository;
import com.yoloFarm.api.service.RuleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RuleServiceConditionPairingTest {

    @Mock
    private RuleRepository ruleRepository;

    @Mock
    private FarmRepository farmRepository;

    @Mock
    private DeviceRepository deviceRepository;

    @InjectMocks
    private RuleService ruleService;

    private UUID ownerId;
    private UUID farmId;
    private UUID triggerDeviceId;
    private UUID actionDeviceId;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
        farmId = UUID.randomUUID();
        triggerDeviceId = UUID.randomUUID();
        actionDeviceId = UUID.randomUUID();
    }

    @Test
    void shouldCreateInactiveConditionRule_whenOppositeRuleIsMissing() {
        Farm farm = buildFarm();
        Device trigger = buildSensorDevice(triggerDeviceId, farm);
        Device action = buildActuatorDevice(actionDeviceId, farm);

        RuleCreateRequest request = buildConditionRequest(ActionCommandEnum.ON);

        when(farmRepository.findByIdAndOwnerId(farmId, ownerId)).thenReturn(Optional.of(farm));
        when(deviceRepository.findByIdAndFarmOwnerId(triggerDeviceId, ownerId)).thenReturn(Optional.of(trigger));
        when(deviceRepository.findByIdAndFarmOwnerId(actionDeviceId, ownerId)).thenReturn(Optional.of(action));
        when(ruleRepository.findByFarmIdAndActionDeviceIdAndTriggerDeviceIdAndRuleTypeAndActionCommand(
                eq(farmId),
                eq(actionDeviceId),
                eq(triggerDeviceId),
                eq(RuleTypeEnum.CONDITION),
                eq(ActionCommandEnum.OFF)))
                .thenReturn(List.of());
        when(ruleRepository.save(any(Rule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RuleResponse response = ruleService.createRule(request, ownerId);

        assertFalse(response.getIsActive());
    }

    @Test
    void shouldActivateBothConditionRules_whenValidOppositeExistsOnCreate() {
        Farm farm = buildFarm();
        Device trigger = buildSensorDevice(triggerDeviceId, farm);
        Device action = buildActuatorDevice(actionDeviceId, farm);

        RuleCreateRequest request = buildConditionRequest(ActionCommandEnum.ON);
        Rule opposite = buildConditionRuleWithShape(farm, action, trigger, ActionCommandEnum.OFF, ">", 40f, false);

        when(farmRepository.findByIdAndOwnerId(farmId, ownerId)).thenReturn(Optional.of(farm));
        when(deviceRepository.findByIdAndFarmOwnerId(triggerDeviceId, ownerId)).thenReturn(Optional.of(trigger));
        when(deviceRepository.findByIdAndFarmOwnerId(actionDeviceId, ownerId)).thenReturn(Optional.of(action));
        when(ruleRepository.findByFarmIdAndActionDeviceIdAndTriggerDeviceIdAndRuleTypeAndActionCommand(
                eq(farmId),
                eq(actionDeviceId),
                eq(triggerDeviceId),
                eq(RuleTypeEnum.CONDITION),
                eq(ActionCommandEnum.OFF)))
                .thenReturn(List.of(opposite));
        when(ruleRepository.save(any(Rule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RuleResponse response = ruleService.createRule(request, ownerId);

        assertTrue(response.getIsActive());
        assertTrue(opposite.getIsActive());
    }

    @Test
    void shouldThrowWhenActivatingConditionRule_withoutOppositeRule() {
        Rule existing = buildExistingConditionRule(ActionCommandEnum.ON, false);
        existing.setTriggerDevice(buildSensorDevice(triggerDeviceId, existing.getFarm()));

        when(ruleRepository.findByIdAndFarmOwnerId(existing.getId(), ownerId)).thenReturn(Optional.of(existing));
        when(ruleRepository.findByFarmIdAndActionDeviceIdAndTriggerDeviceIdAndRuleTypeAndActionCommand(
                eq(farmId),
                eq(actionDeviceId),
                eq(triggerDeviceId),
                eq(RuleTypeEnum.CONDITION),
                eq(ActionCommandEnum.OFF)))
                .thenReturn(List.of());

        assertThrows(IllegalStateException.class, () -> ruleService.toggleRule(existing.getId(), true, ownerId));
        verify(ruleRepository, never()).save(any(Rule.class));
    }

    @Test
    void shouldActivateBothConditionRules_whenToggleOnWithValidOpposite() {
        Rule existing = buildExistingConditionRule(ActionCommandEnum.ON, false);
        Rule opposite = buildConditionRuleWithShape(
                existing.getFarm(),
                existing.getActionDevice(),
                existing.getTriggerDevice(),
                ActionCommandEnum.OFF,
                ">",
                40f,
                false);

        when(ruleRepository.findByIdAndFarmOwnerId(existing.getId(), ownerId)).thenReturn(Optional.of(existing));
        when(ruleRepository.findByFarmIdAndActionDeviceIdAndTriggerDeviceIdAndRuleTypeAndActionCommand(
                eq(farmId),
                eq(actionDeviceId),
                eq(triggerDeviceId),
                eq(RuleTypeEnum.CONDITION),
                eq(ActionCommandEnum.OFF)))
                .thenReturn(List.of(opposite));
        when(ruleRepository.save(any(Rule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RuleResponse response = ruleService.toggleRule(existing.getId(), true, ownerId);

        assertTrue(response.getIsActive());
        assertTrue(opposite.getIsActive());
    }

    @Test
    void shouldDeactivateOppositeConditionRule_whenTogglingOff() {
        Rule existing = buildExistingConditionRule(ActionCommandEnum.ON, true);
        Rule opposite = buildConditionRuleWithShape(
                existing.getFarm(),
                existing.getActionDevice(),
                existing.getTriggerDevice(),
                ActionCommandEnum.OFF,
                ">",
                40f,
                true);

        when(ruleRepository.findByIdAndFarmOwnerId(existing.getId(), ownerId)).thenReturn(Optional.of(existing));
        when(ruleRepository.findByFarmIdAndActionDeviceIdAndTriggerDeviceIdAndRuleTypeAndActionCommand(
                eq(farmId),
                eq(actionDeviceId),
                eq(triggerDeviceId),
                eq(RuleTypeEnum.CONDITION),
                eq(ActionCommandEnum.OFF)))
                .thenReturn(List.of(opposite));
        when(ruleRepository.save(any(Rule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RuleResponse response = ruleService.toggleRule(existing.getId(), false, ownerId);

        assertFalse(response.getIsActive());
        assertFalse(opposite.getIsActive());
    }

    @Test
    void shouldThrowWhenActivatingConditionRule_withConflictingPairLogic() {
        Rule existing = buildExistingConditionRule(ActionCommandEnum.ON, false);
        Rule opposite = buildConditionRuleWithShape(
                existing.getFarm(),
                existing.getActionDevice(),
                existing.getTriggerDevice(),
                ActionCommandEnum.OFF,
                "<",
                20f,
                false);

        when(ruleRepository.findByIdAndFarmOwnerId(existing.getId(), ownerId)).thenReturn(Optional.of(existing));
        when(ruleRepository.findByFarmIdAndActionDeviceIdAndTriggerDeviceIdAndRuleTypeAndActionCommand(
                eq(farmId),
                eq(actionDeviceId),
                eq(triggerDeviceId),
                eq(RuleTypeEnum.CONDITION),
                eq(ActionCommandEnum.OFF)))
                .thenReturn(List.of(opposite));

        assertThrows(IllegalStateException.class, () -> ruleService.toggleRule(existing.getId(), true, ownerId));
        verify(ruleRepository, never()).save(any(Rule.class));
    }

    private RuleCreateRequest buildConditionRequest(ActionCommandEnum command) {
        RuleCreateRequest request = new RuleCreateRequest();
        request.setFarmId(farmId);
        request.setRuleName("Auto rule");
        request.setRuleType(RuleTypeEnum.CONDITION);
        request.setTriggerDeviceId(triggerDeviceId);
        request.setOperator("<");
        request.setThresholdValue(30f);
        request.setActionDeviceId(actionDeviceId);
        request.setActionCommand(command);
        return request;
    }

    private Rule buildExistingConditionRule(ActionCommandEnum command, boolean active) {
        Farm farm = buildFarm();
        Device trigger = buildSensorDevice(triggerDeviceId, farm);
        Device action = buildActuatorDevice(actionDeviceId, farm);

        Rule rule = new Rule();
        rule.setId(UUID.randomUUID());
        rule.setFarm(farm);
        rule.setTriggerDevice(trigger);
        rule.setActionDevice(action);
        rule.setActionCommand(command);
        rule.setRuleType(RuleTypeEnum.CONDITION);
        rule.setRuleName("Condition Rule");
        rule.setOperator("<");
        rule.setThresholdValue(30f);
        rule.setIsActive(active);
        return rule;
    }

    private Rule buildConditionRuleWithShape(
            Farm farm,
            Device actionDevice,
            Device triggerDevice,
            ActionCommandEnum command,
            String operator,
            float threshold,
            boolean isActive) {
        Rule rule = new Rule();
        rule.setId(UUID.randomUUID());
        rule.setFarm(farm);
        rule.setTriggerDevice(triggerDevice);
        rule.setActionDevice(actionDevice);
        rule.setActionCommand(command);
        rule.setRuleType(RuleTypeEnum.CONDITION);
        rule.setRuleName("Complementary Condition Rule");
        rule.setOperator(operator);
        rule.setThresholdValue(threshold);
        rule.setIsActive(isActive);
        return rule;
    }

    private Farm buildFarm() {
        User owner = User.builder()
                .id(ownerId)
                .username("owner")
                .password("pwd")
                .email("owner@yolo.test")
                .role(RoleEnum.FARMER)
                .build();

        Farm farm = new Farm();
        farm.setId(farmId);
        farm.setOwner(owner);
        return farm;
    }

    private Device buildSensorDevice(UUID id, Farm farm) {
        DeviceModel model = DeviceModel.builder()
                .id(UUID.randomUUID())
                .modelName("Soil sensor")
                .deviceType(DeviceTypeEnum.SENSOR)
                .metricType(MetricTypeEnum.SOIL_MOISTURE)
                .build();

        Device device = new Device();
        device.setId(id);
        device.setFarm(farm);
        device.setModel(model);
        return device;
    }

    private Device buildActuatorDevice(UUID id, Farm farm) {
        DeviceModel model = DeviceModel.builder()
                .id(UUID.randomUUID())
                .modelName("Pump")
                .deviceType(DeviceTypeEnum.ACTUATOR)
                .metricType(MetricTypeEnum.PUMP)
                .build();

        Device device = new Device();
        device.setId(id);
        device.setFarm(farm);
        device.setModel(model);
        return device;
    }
}
