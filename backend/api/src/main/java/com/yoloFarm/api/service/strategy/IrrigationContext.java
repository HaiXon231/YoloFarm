package com.yoloFarm.api.service.strategy;

import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class IrrigationContext {

    private IrrigationStrategy strategy;

    public void setStrategy(IrrigationStrategy strategy) {
        this.strategy = strategy;
    }

    public boolean executeControl(UUID farmId, UUID deviceId, String command) {
        if (strategy == null) {
            throw new IllegalStateException("Strategy chưa được khởi tạo!");
        }
        return strategy.executeControl(farmId, deviceId, command);
    }
}
