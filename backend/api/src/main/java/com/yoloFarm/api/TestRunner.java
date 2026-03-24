package com.yoloFarm.api;

import com.yoloFarm.api.service.strategy.IrrigationContext;
import com.yoloFarm.api.service.strategy.ManualStrategy; // Import Strategy
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TestRunner implements CommandLineRunner {

    private final IrrigationContext irrigationContext;
    private final ManualStrategy manualStrategy;

    // Inject cả Context và Strategy vào
    public TestRunner(IrrigationContext irrigationContext, ManualStrategy manualStrategy) {
        this.irrigationContext = irrigationContext;
        this.manualStrategy = manualStrategy;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("========== BẮT ĐẦU TEST STRATEGY PATTERN ==========");

        UUID dummyFarmId = UUID.randomUUID();
        UUID dummyDeviceId = UUID.randomUUID();

        // Đong vai trò như 1 Client ra lệnh, ta set chế độ sang Hằng tay (Manual)
        irrigationContext.setStrategy(manualStrategy);

        // Phát lệnh kích hoạt
        irrigationContext.executeControl(dummyFarmId, dummyDeviceId, "ON");

        System.out.println("========== KẾT THÚC TEST STRATEGY PATTERN ==========");
    }
}
