package com.yoloFarm.api.service.strategy;

import java.util.UUID;

public interface IrrigationStrategy {
    boolean executeControl(UUID farmId, UUID deviceId, String command);
}
