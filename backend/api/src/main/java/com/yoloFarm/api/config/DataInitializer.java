package com.yoloFarm.api.config;

import com.yoloFarm.api.entity.DeviceModel;
import com.yoloFarm.api.entity.User;
import com.yoloFarm.api.enums.DeviceTypeEnum;
import com.yoloFarm.api.enums.MetricTypeEnum;
import com.yoloFarm.api.enums.RoleEnum;
import com.yoloFarm.api.repository.DeviceModelRepository;
import com.yoloFarm.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Seeds default admin account and device models on first startup.
 * Idempotent — checks for existing data before inserting.
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final DeviceModelRepository deviceModelRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedAdmin();
        seedModels();
    }

    private void seedAdmin() {
        if (userRepository.findByUsername("admin").isPresent()) {
            log.info("DataInitializer: admin user already exists, skipping.");
            return;
        }
        User admin = User.builder()
                .username("admin")
                .email("admin@yolofarm.com")
                .password(passwordEncoder.encode("Admin@1234"))
                .role(RoleEnum.ADMIN)
                .build();
        userRepository.save(admin);
        log.info("DataInitializer: created admin user (admin / Admin@1234).");
    }

    private void seedModels() {
        if (deviceModelRepository.count() > 0) {
            log.info("DataInitializer: device models already exist, skipping.");
            return;
        }
        List<DeviceModel> models = List.of(
                DeviceModel.builder().modelName("Temperature Sensor").manufacturer("YoloFarm")
                        .deviceType(DeviceTypeEnum.SENSOR).metricType(MetricTypeEnum.TEMP).build(),
                DeviceModel.builder().modelName("Humidity Sensor").manufacturer("YoloFarm")
                        .deviceType(DeviceTypeEnum.SENSOR).metricType(MetricTypeEnum.HUMIDITY).build(),
                DeviceModel.builder().modelName("Soil Moisture Sensor").manufacturer("YoloFarm")
                        .deviceType(DeviceTypeEnum.SENSOR).metricType(MetricTypeEnum.SOIL_MOISTURE).build(),
                DeviceModel.builder().modelName("Light Sensor").manufacturer("YoloFarm")
                        .deviceType(DeviceTypeEnum.SENSOR).metricType(MetricTypeEnum.LIGHT).build(),
                DeviceModel.builder().modelName("Water Pump").manufacturer("YoloFarm")
                        .deviceType(DeviceTypeEnum.ACTUATOR).metricType(MetricTypeEnum.PUMP).build()
        );
        deviceModelRepository.saveAll(models);
        log.info("DataInitializer: created {} device models.", models.size());
    }
}
