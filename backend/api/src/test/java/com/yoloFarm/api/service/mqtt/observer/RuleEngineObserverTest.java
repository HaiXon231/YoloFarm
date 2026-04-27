package com.yoloFarm.api.service.mqtt.observer;

import com.yoloFarm.api.dto.SensorData;
import com.yoloFarm.api.entity.Device;
import com.yoloFarm.api.entity.Farm;
import com.yoloFarm.api.entity.Rule;
import com.yoloFarm.api.entity.User;
import com.yoloFarm.api.enums.ActionCommandEnum;
import com.yoloFarm.api.repository.RuleRepository;
import com.yoloFarm.api.service.NotificationService;
import com.yoloFarm.api.service.automation.AutomationRuntimeStateService;
import com.yoloFarm.api.service.strategy.AutoThresholdStrategy;
import com.yoloFarm.api.service.strategy.IrrigationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RuleEngineObserverTest {

    @Mock
    private RuleRepository ruleRepository;
    @Mock
    private IrrigationContext irrigationContext;
    @Mock
    private AutoThresholdStrategy autoThresholdStrategy;
    @Mock
    private NotificationService notificationService;
    @Mock
    private AutomationRuntimeStateService automationRuntimeStateService;

    @InjectMocks
    private RuleEngineObserver ruleEngineObserver;

    private Clock fixedClock;
    private Instant now;

    @BeforeEach
    void setUp() {
        now = Instant.parse("2026-04-27T10:00:00Z");
        fixedClock = Clock.fixed(now, ZoneId.of("UTC"));
        ReflectionTestUtils.setField(ruleEngineObserver, "clock", fixedClock);
        ReflectionTestUtils.setField(ruleEngineObserver, "ruleCommandCooldownSeconds", 30L);
    }

    @Test
    void update_WhenConditionMetAndNotInCooldown_ShouldExecuteCommand() {
        UUID triggerDeviceId = UUID.randomUUID();
        SensorData data = new SensorData(UUID.randomUUID(), triggerDeviceId, "TEMP", 45.0f, now);

        UUID actionDeviceId = UUID.randomUUID();
        Device actionDevice = new Device();
        actionDevice.setId(actionDeviceId);
        actionDevice.setName("Pump 1");
        actionDevice.setIsActive(false);

        UUID ownerId = UUID.randomUUID();
        User owner = new User();
        owner.setId(ownerId);

        UUID farmId = UUID.randomUUID();
        Farm farm = new Farm();
        farm.setId(farmId);
        farm.setOwner(owner);

        UUID ruleId = UUID.randomUUID();
        Rule rule = new Rule();
        rule.setId(ruleId);
        rule.setOperator(">");
        rule.setThresholdValue(40.0f);
        rule.setActionCommand(ActionCommandEnum.ON);
        rule.setActionDevice(actionDevice);
        rule.setFarm(farm);

        when(ruleRepository.findActiveRulesWithAssociations(triggerDeviceId)).thenReturn(List.of(rule));
        when(automationRuntimeStateService.isRuleCommandInCooldown(eq(ruleId), eq("ON"), eq(30L), eq(now)))
                .thenReturn(false);
        when(irrigationContext.executeControl(autoThresholdStrategy, farmId, actionDeviceId, "ON"))
                .thenReturn(true);

        ruleEngineObserver.update(data);

        verify(irrigationContext).executeControl(autoThresholdStrategy, farmId, actionDeviceId, "ON");
        verify(automationRuntimeStateService).markRuleCommandExecuted(ruleId, "ON", now);
        verify(automationRuntimeStateService).markAutoCommand(actionDeviceId, "ON", now);
        verify(notificationService).createSystemNotification(eq(ownerId), contains("triggered by"));
    }

    @Test
    void update_WhenConditionNotMet_ShouldDoNothing() {
        UUID triggerDeviceId = UUID.randomUUID();
        SensorData data = new SensorData(UUID.randomUUID(), triggerDeviceId, "TEMP", 35.0f, now);

        Rule rule = new Rule();
        rule.setOperator(">");
        rule.setThresholdValue(40.0f);

        when(ruleRepository.findActiveRulesWithAssociations(triggerDeviceId)).thenReturn(List.of(rule));

        ruleEngineObserver.update(data);

        verify(irrigationContext, never()).executeControl(any(), any(), any(), any());
    }

    @Test
    void update_WhenInCooldown_ShouldDoNothing() {
        UUID triggerDeviceId = UUID.randomUUID();
        SensorData data = new SensorData(UUID.randomUUID(), triggerDeviceId, "TEMP", 45.0f, now);

        Device actionDevice = new Device();
        actionDevice.setIsActive(false);

        UUID ruleId = UUID.randomUUID();
        Rule rule = new Rule();
        rule.setId(ruleId);
        rule.setOperator(">");
        rule.setThresholdValue(40.0f);
        rule.setActionCommand(ActionCommandEnum.ON);
        rule.setActionDevice(actionDevice);

        when(ruleRepository.findActiveRulesWithAssociations(triggerDeviceId)).thenReturn(List.of(rule));
        when(automationRuntimeStateService.isRuleCommandInCooldown(eq(ruleId), eq("ON"), eq(30L), eq(now)))
                .thenReturn(true);

        ruleEngineObserver.update(data);

        verify(irrigationContext, never()).executeControl(any(), any(), any(), any());
    }
}
