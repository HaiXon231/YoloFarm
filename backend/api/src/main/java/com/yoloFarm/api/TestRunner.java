package com.yoloFarm.api;

import com.yoloFarm.api.service.strategy.IrrigationContext;
import com.yoloFarm.api.service.strategy.ManualStrategy;
import org.springframework.boot.CommandLineRunner;

import java.util.UUID;

// @Component // Đã đóng lại để tránh Spring Context tự động kích hoạt làm vỡ các Integration Test khác
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

        // Strategy truyền qua tham số, không dùng setStrategy() nữa
        irrigationContext.executeControl(manualStrategy, dummyFarmId, dummyDeviceId, "ON");

        System.out.println("========== KẾT THÚC TEST STRATEGY PATTERN ==========");
    }
}
