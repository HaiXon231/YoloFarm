package com.yoloFarm.api.service.mqtt.observer;

import com.yoloFarm.api.dto.SensorData;
import com.yoloFarm.api.entity.Rule;
import com.yoloFarm.api.repository.RuleRepository;
import com.yoloFarm.api.service.strategy.AutoThresholdStrategy;
import com.yoloFarm.api.service.strategy.IrrigationContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class RuleEngineObserver implements Observer {

    private final RuleRepository ruleRepository;
    private final IrrigationContext irrigationContext;
    private final AutoThresholdStrategy autoThresholdStrategy;

    @Override
    public void update(SensorData data) {
        log.info("RuleEngineObserver nhận dữ liệu: {} = {}", data.metricType(), data.value());
        
        List<Rule> rules = ruleRepository.findByTriggerDeviceIdAndIsActiveTrue(data.deviceId());
        
        for (Rule rule : rules) {
            boolean conditionMet = evaluateCondition(data.value(), rule.getOperator(), rule.getThresholdValue());
            
            if (conditionMet) {
                log.info("Rule Triggered: Đã tự động bật/tắt thiết bị dựa trên cảm biến ({} {} {})", 
                         data.value(), rule.getOperator(), rule.getThresholdValue());
                
                // Strategy truyền qua tham số, không dùng setStrategy() nữa
                irrigationContext.executeControl(
                        autoThresholdStrategy,
                        rule.getFarm().getId(),
                        rule.getActionDevice().getId(),
                        rule.getActionCommand().name()
                );
            }
        }
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
