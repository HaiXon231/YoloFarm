package com.yoloFarm.api.config;

import com.yoloFarm.api.entity.AppSetting;
import com.yoloFarm.api.entity.Device;
import com.yoloFarm.api.entity.DeviceModel;
import com.yoloFarm.api.entity.User;
import com.yoloFarm.api.enums.DeviceTypeEnum;
import com.yoloFarm.api.enums.MetricTypeEnum;
import com.yoloFarm.api.enums.RoleEnum;
import com.yoloFarm.api.repository.AppSettingRepository;
import com.yoloFarm.api.repository.DeviceModelRepository;
import com.yoloFarm.api.repository.DeviceRepository;
import com.yoloFarm.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final DeviceModelRepository deviceModelRepository;
    private final DeviceRepository deviceRepository;
    private final AppSettingRepository appSettingRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(String... args) {
        seedAdmin();
        seedAutomationSettings();
        refreshModelCheckConstraints();
        upsertModels();
        backfillDeviceThresholds();
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

    private void seedAutomationSettings() {
        createSettingIfMissing("automation.max_auto_on_minutes", "20");
        createSettingIfMissing("automation.command_cooldown_seconds", "30");
    }

    private void refreshModelCheckConstraints() {
        jdbcTemplate.execute("ALTER TABLE models DROP CONSTRAINT IF EXISTS models_metric_type_check");
        jdbcTemplate.execute("""
                ALTER TABLE models ADD CONSTRAINT models_metric_type_check
                CHECK (metric_type IN (
                    'TEMP',
                    'HUMIDITY',
                    'SOIL_MOISTURE',
                    'LIGHT',
                    'PRESSURE',
                    'CO2',
                    'PUMP',
                    'VALVE',
                    'RELAY',
                    'FAN'
                ))
                """);
    }

    private void createSettingIfMissing(String key, String value) {
        if (appSettingRepository.existsById(key)) {
            return;
        }
        appSettingRepository.save(AppSetting.builder().key(key).value(value).build());
    }

    private void upsertModels() {
        Map<String, DeviceModel> existingByName = deviceModelRepository.findAll().stream()
                .collect(Collectors.toMap(
                        model -> model.getModelName().toLowerCase(),
                        Function.identity(),
                        (left, right) -> left));

        List<DeviceModel> models = modelSeeds().stream()
                .map(seed -> {
                    DeviceModel model = existingByName.getOrDefault(
                            seed.modelName().toLowerCase(),
                            DeviceModel.builder().modelName(seed.modelName()).build());
                    model.setDeviceType(seed.deviceType());
                    model.setMetricType(seed.metricType());
                    model.setManufacturer(seed.manufacturer());
                    model.setDisplayUnit(seed.displayUnit());
                    model.setMinValue(seed.minValue());
                    model.setMaxValue(seed.maxValue());
                    model.setModelDescription(seed.description());
                    model.setReferenceUrl(seed.referenceUrl());
                    return model;
                })
                .toList();

        deviceModelRepository.saveAll(models);
        log.info("DataInitializer: upserted {} device models.", models.size());
    }

    private void backfillDeviceThresholds() {
        List<Device> devices = deviceRepository.findAll().stream()
                .filter(device -> device.getModel() != null)
                .filter(device -> device.getMinValue() == null || device.getMaxValue() == null)
                .peek(device -> {
                    DeviceModel model = device.getModel();
                    if (device.getMinValue() == null) {
                        device.setMinValue(model.getMinValue());
                    }
                    if (device.getMaxValue() == null) {
                        device.setMaxValue(model.getMaxValue());
                    }
                })
                .toList();

        if (!devices.isEmpty()) {
            deviceRepository.saveAll(devices);
            log.info("DataInitializer: backfilled min/max thresholds for {} device(s).", devices.size());
        }
    }

    private List<ModelSeed> modelSeeds() {
        return List.of(
                new ModelSeed("Adafruit SHT31-D Air Temperature Sensor", DeviceTypeEnum.SENSOR, MetricTypeEnum.TEMP,
                        "Adafruit / Sensirion", "degC", 15.0f, 45.0f,
                        "Logical model for SHT31-D temperature feed only; suitable for greenhouse air-temperature mock telemetry.",
                        "https://www.adafruit.com/product/2857"),
                new ModelSeed("Adafruit SHT31-D Air Humidity Sensor", DeviceTypeEnum.SENSOR, MetricTypeEnum.HUMIDITY,
                        "Adafruit / Sensirion", "%", 35.0f, 95.0f,
                        "Logical model for SHT31-D relative-humidity feed only; separate from temperature to match one metric per model.",
                        "https://www.adafruit.com/product/2857"),
                new ModelSeed("Adafruit BME280 Barometric Pressure Sensor", DeviceTypeEnum.SENSOR, MetricTypeEnum.PRESSURE,
                        "Adafruit / Bosch", "hPa", 980.0f, 1035.0f,
                        "Logical model for BME280 pressure feed only; mock range is typical near-surface atmospheric pressure.",
                        "https://www.adafruit.com/product/2652"),
                new ModelSeed("Adafruit BME280 Air Temperature Sensor", DeviceTypeEnum.SENSOR, MetricTypeEnum.TEMP,
                        "Adafruit / Bosch", "degC", 15.0f, 45.0f,
                        "Logical model for BME280 temperature feed only; useful when one physical BME280 board publishes separate feeds.",
                        "https://www.adafruit.com/product/2652"),
                new ModelSeed("Adafruit STEMMA Soil Moisture Sensor", DeviceTypeEnum.SENSOR, MetricTypeEnum.SOIL_MOISTURE,
                        "Adafruit", "%", 10.0f, 85.0f,
                        "Logical model for soil-moisture percentage after calibration; mock values represent dry-to-wet agricultural soil.",
                        "https://www.adafruit.com/product/4026"),
                new ModelSeed("Adafruit STEMMA Soil Temperature Sensor", DeviceTypeEnum.SENSOR, MetricTypeEnum.TEMP,
                        "Adafruit", "degC", 18.0f, 38.0f,
                        "Logical model for soil-temperature feed only; kept separate from soil moisture.",
                        "https://www.adafruit.com/product/4026"),
                new ModelSeed("Adafruit Waterproof DS18B20 Water Temperature Sensor", DeviceTypeEnum.SENSOR, MetricTypeEnum.TEMP,
                        "Adafruit / Maxim Integrated", "degC", 10.0f, 40.0f,
                        "Logical model for water or nutrient-solution temperature telemetry.",
                        "https://www.adafruit.com/product/381"),
                new ModelSeed("Adafruit VEML7700 Ambient Light Sensor", DeviceTypeEnum.SENSOR, MetricTypeEnum.LIGHT,
                        "Adafruit / Vishay", "lux", 50.0f, 90000.0f,
                        "Logical model for ambient light intensity; mock range supports indoor greenhouse to strong daylight.",
                        "https://www.adafruit.com/product/4162"),
                new ModelSeed("Adafruit TSL2591 High Range Light Sensor", DeviceTypeEnum.SENSOR, MetricTypeEnum.LIGHT,
                        "Adafruit / AMS", "lux", 10.0f, 100000.0f,
                        "Logical model for high dynamic range light telemetry.",
                        "https://www.adafruit.com/product/1980"),
                new ModelSeed("Adafruit SCD-40 CO2 Sensor", DeviceTypeEnum.SENSOR, MetricTypeEnum.CO2,
                        "Adafruit / Sensirion", "ppm", 400.0f, 2000.0f,
                        "Logical model for CO2 concentration feed only; mock range fits greenhouse ventilation scenarios.",
                        "https://www.adafruit.com/product/5187"),
                new ModelSeed("Adafruit Peristaltic Liquid Pump 12V", DeviceTypeEnum.ACTUATOR, MetricTypeEnum.PUMP,
                        "Adafruit", "ON_OFF", 0.0f, 1.0f,
                        "Logical pump actuator state; 0 means OFF, 1 means ON.",
                        "https://www.adafruit.com/product/1150"),
                new ModelSeed("Adafruit Peristaltic Liquid Pump 5V", DeviceTypeEnum.ACTUATOR, MetricTypeEnum.PUMP,
                        "Adafruit", "ON_OFF", 0.0f, 1.0f,
                        "Logical small pump actuator state; suitable for nutrient dosing or small irrigation mock control.",
                        "https://www.adafruit.com/product/3910"),
                new ModelSeed("Adafruit Submersible Water Pump 3V", DeviceTypeEnum.ACTUATOR, MetricTypeEnum.PUMP,
                        "Adafruit", "ON_OFF", 0.0f, 1.0f,
                        "Logical submersible pump state; 0 means OFF, 1 means ON.",
                        "https://www.adafruit.com/product/4546"),
                new ModelSeed("Adafruit Brass Liquid Solenoid Valve 12V", DeviceTypeEnum.ACTUATOR, MetricTypeEnum.VALVE,
                        "Adafruit", "OPEN_CLOSED", 0.0f, 1.0f,
                        "Logical irrigation valve state; 0 means CLOSED, 1 means OPEN.",
                        "https://www.adafruit.com/product/996"),
                new ModelSeed("Adafruit Plastic Water Solenoid Valve 12V", DeviceTypeEnum.ACTUATOR, MetricTypeEnum.VALVE,
                        "Adafruit", "OPEN_CLOSED", 0.0f, 1.0f,
                        "Logical water valve state for irrigation control; 0 means CLOSED, 1 means OPEN.",
                        "https://www.adafruit.com/product/997"),
                new ModelSeed("Adafruit Power Relay FeatherWing", DeviceTypeEnum.ACTUATOR, MetricTypeEnum.RELAY,
                        "Adafruit", "ON_OFF", 0.0f, 1.0f,
                        "Logical relay state for controlling external loads; 0 means OFF, 1 means ON.",
                        "https://www.adafruit.com/product/3191"),
                new ModelSeed("Adafruit STEMMA MOSFET Driver", DeviceTypeEnum.ACTUATOR, MetricTypeEnum.RELAY,
                        "Adafruit", "ON_OFF", 0.0f, 1.0f,
                        "Logical MOSFET switch state for solenoids, pumps, LEDs, or fans; 0 means OFF, 1 means ON.",
                        "https://www.adafruit.com/product/5648"),
                new ModelSeed("Adafruit Miniature 5V Cooling Fan", DeviceTypeEnum.ACTUATOR, MetricTypeEnum.FAN,
                        "Adafruit", "ON_OFF", 0.0f, 1.0f,
                        "Logical fan state for greenhouse ventilation; 0 means OFF, 1 means ON.",
                        "https://www.adafruit.com/product/3368")
        );
    }

    private record ModelSeed(
            String modelName,
            DeviceTypeEnum deviceType,
            MetricTypeEnum metricType,
            String manufacturer,
            String displayUnit,
            Float minValue,
            Float maxValue,
            String description,
            String referenceUrl) {
    }
}
