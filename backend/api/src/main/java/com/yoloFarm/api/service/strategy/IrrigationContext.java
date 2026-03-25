package com.yoloFarm.api.service.strategy;

import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class IrrigationContext {

    /**
     * Strategy được truyền vào qua tham số (nằm trên Stack riêng mỗi thread),
     * thay vì lưu trong instance field (Heap chung) → tránh race condition.
     */
    public boolean executeControl(IrrigationStrategy strategy, UUID farmId, UUID deviceId, String command) {
        if (strategy == null) {
            throw new IllegalStateException("Strategy chưa được khởi tạo!");
        }
        return strategy.executeControl(farmId, deviceId, command);
    }
}

