-- Seed realistic Adafruit-compatible device models for YoloFarm.
--
-- Design decisions:
-- 1. Do not insert `id`; UUID is generated outside this seed.
-- 2. Each row represents exactly one metric/control data type.
-- 3. Multi-metric boards are split into one logical model per feed/metric.
-- 4. display_unit + min_value + max_value support UI display and digital-twin mock data.
--
-- If your Java enums do not yet include PRESSURE, SOIL_MOISTURE, LIGHT, CO2,
-- VALVE, RELAY, FAN, add them before running this seed or remove those rows.

BEGIN;

-- Metadata columns for frontend display and simulator/mock value generation.
ALTER TABLE models ADD COLUMN IF NOT EXISTS display_unit VARCHAR(20);
ALTER TABLE models ADD COLUMN IF NOT EXISTS min_value DOUBLE PRECISION;
ALTER TABLE models ADD COLUMN IF NOT EXISTS max_value DOUBLE PRECISION;
ALTER TABLE models ADD COLUMN IF NOT EXISTS model_description TEXT;
ALTER TABLE models ADD COLUMN IF NOT EXISTS reference_url TEXT;

-- Prevent duplicate model catalog rows across repeated local seeding.
CREATE UNIQUE INDEX IF NOT EXISTS ux_models_model_name_lower
ON models (LOWER(model_name));

-- Adafruit SHT31-D Air Temperature Sensor | unit: degC | mock range: 15.0 - 45.0
INSERT INTO models (
    model_name,
    device_type,
    metric_type,
    manufacturer,
    display_unit,
    min_value,
    max_value,
    model_description,
    reference_url
)
VALUES (
    'Adafruit SHT31-D Air Temperature Sensor',
    'SENSOR',
    'TEMP',
    'Adafruit / Sensirion',
    'degC',
    15.0,
    45.0,
    'Logical model for SHT31-D temperature feed only; suitable for greenhouse air-temperature mock telemetry.',
    'https://www.adafruit.com/product/2857'
)
ON CONFLICT ((LOWER(model_name))) DO UPDATE SET
    device_type = EXCLUDED.device_type,
    metric_type = EXCLUDED.metric_type,
    manufacturer = EXCLUDED.manufacturer,
    display_unit = EXCLUDED.display_unit,
    min_value = EXCLUDED.min_value,
    max_value = EXCLUDED.max_value,
    
    model_description = EXCLUDED.model_description,
    reference_url = EXCLUDED.reference_url;

-- Adafruit SHT31-D Air Humidity Sensor | unit: % | mock range: 35.0 - 95.0
INSERT INTO models (
    model_name,
    device_type,
    metric_type,
    manufacturer,
    display_unit,
    min_value,
    max_value,
    
    model_description,
    reference_url
)
VALUES (
    'Adafruit SHT31-D Air Humidity Sensor',
    'SENSOR',
    'HUMIDITY',
    'Adafruit / Sensirion',
    '%',
    35.0,
    95.0,
    'Logical model for SHT31-D relative-humidity feed only; separate from temperature to match one metric per model.',
    'https://www.adafruit.com/product/2857'
)
ON CONFLICT ((LOWER(model_name))) DO UPDATE SET
    device_type = EXCLUDED.device_type,
    metric_type = EXCLUDED.metric_type,
    manufacturer = EXCLUDED.manufacturer,
    display_unit = EXCLUDED.display_unit,
    min_value = EXCLUDED.min_value,
    max_value = EXCLUDED.max_value,
    
    model_description = EXCLUDED.model_description,
    reference_url = EXCLUDED.reference_url;

-- Adafruit BME280 Barometric Pressure Sensor | unit: hPa | mock range: 980.0 - 1035.0
INSERT INTO models (
    model_name,
    device_type,
    metric_type,
    manufacturer,
    display_unit,
    min_value,
    max_value,
    
    model_description,
    reference_url
)
VALUES (
    'Adafruit BME280 Barometric Pressure Sensor',
    'SENSOR',
    'PRESSURE',
    'Adafruit / Bosch',
    'hPa',
    980.0,
    1035.0,
    'Logical model for BME280 pressure feed only; mock range is typical near-surface atmospheric pressure.',
    'https://www.adafruit.com/product/2652'
)
ON CONFLICT ((LOWER(model_name))) DO UPDATE SET
    device_type = EXCLUDED.device_type,
    metric_type = EXCLUDED.metric_type,
    manufacturer = EXCLUDED.manufacturer,
    display_unit = EXCLUDED.display_unit,
    min_value = EXCLUDED.min_value,
    max_value = EXCLUDED.max_value,
    
    model_description = EXCLUDED.model_description,
    reference_url = EXCLUDED.reference_url;

-- Adafruit BME280 Air Temperature Sensor | unit: degC | mock range: 15.0 - 45.0
INSERT INTO models (
    model_name,
    device_type,
    metric_type,
    manufacturer,
    display_unit,
    min_value,
    max_value,
    
    model_description,
    reference_url
)
VALUES (
    'Adafruit BME280 Air Temperature Sensor',
    'SENSOR',
    'TEMP',
    'Adafruit / Bosch',
    'degC',
    15.0,
    45.0,
    'Logical model for BME280 temperature feed only; useful when one physical BME280 board publishes separate feeds.',
    'https://www.adafruit.com/product/2652'
)
ON CONFLICT ((LOWER(model_name))) DO UPDATE SET
    device_type = EXCLUDED.device_type,
    metric_type = EXCLUDED.metric_type,
    manufacturer = EXCLUDED.manufacturer,
    display_unit = EXCLUDED.display_unit,
    min_value = EXCLUDED.min_value,
    max_value = EXCLUDED.max_value,
    
    model_description = EXCLUDED.model_description,
    reference_url = EXCLUDED.reference_url;

-- Adafruit STEMMA Soil Moisture Sensor | unit: % | mock range: 10.0 - 85.0
INSERT INTO models (
    model_name,
    device_type,
    metric_type,
    manufacturer,
    display_unit,
    min_value,
    max_value,
    
    model_description,
    reference_url
)
VALUES (
    'Adafruit STEMMA Soil Moisture Sensor',
    'SENSOR',
    'SOIL_MOISTURE',
    'Adafruit',
    '%',
    10.0,
    85.0,
    'Logical model for soil-moisture percentage after calibration; mock values represent dry-to-wet agricultural soil.',
    'https://www.adafruit.com/product/4026'
)
ON CONFLICT ((LOWER(model_name))) DO UPDATE SET
    device_type = EXCLUDED.device_type,
    metric_type = EXCLUDED.metric_type,
    manufacturer = EXCLUDED.manufacturer,
    display_unit = EXCLUDED.display_unit,
    min_value = EXCLUDED.min_value,
    max_value = EXCLUDED.max_value,
    model_description = EXCLUDED.model_description,
    reference_url = EXCLUDED.reference_url;

-- Adafruit STEMMA Soil Temperature Sensor | unit: degC | mock range: 18.0 - 38.0
INSERT INTO models (
    model_name,
    device_type,
    metric_type,
    manufacturer,
    display_unit,
    min_value,
    max_value,
    
    model_description,
    reference_url
)
VALUES (
    'Adafruit STEMMA Soil Temperature Sensor',
    'SENSOR',
    'TEMP',
    'Adafruit',
    'degC',
    18.0,
    38.0,
    'Logical model for soil-temperature feed only; kept separate from soil moisture.',
    'https://www.adafruit.com/product/4026'
)
ON CONFLICT ((LOWER(model_name))) DO UPDATE SET
    device_type = EXCLUDED.device_type,
    metric_type = EXCLUDED.metric_type,
    manufacturer = EXCLUDED.manufacturer,
    display_unit = EXCLUDED.display_unit,
    min_value = EXCLUDED.min_value,
    max_value = EXCLUDED.max_value,
    
    model_description = EXCLUDED.model_description,
    reference_url = EXCLUDED.reference_url;

-- Adafruit Waterproof DS18B20 Water Temperature Sensor | unit: degC | mock range: 10.0 - 40.0
INSERT INTO models (
    model_name,
    device_type,
    metric_type,
    manufacturer,
    display_unit,
    min_value,
    max_value,
    
    model_description,
    reference_url
)
VALUES (
    'Adafruit Waterproof DS18B20 Water Temperature Sensor',
    'SENSOR',
    'TEMP',
    'Adafruit / Maxim Integrated',
    'degC',
    10.0,
    40.0,
    'Logical model for water or nutrient-solution temperature telemetry.',
    'https://www.adafruit.com/product/381'
)
ON CONFLICT ((LOWER(model_name))) DO UPDATE SET
    device_type = EXCLUDED.device_type,
    metric_type = EXCLUDED.metric_type,
    manufacturer = EXCLUDED.manufacturer,
    display_unit = EXCLUDED.display_unit,
    min_value = EXCLUDED.min_value,
    max_value = EXCLUDED.max_value,
    
    model_description = EXCLUDED.model_description,
    reference_url = EXCLUDED.reference_url;

-- Adafruit VEML7700 Ambient Light Sensor | unit: lux | mock range: 50.0 - 90000.0
INSERT INTO models (
    model_name,
    device_type,
    metric_type,
    manufacturer,
    display_unit,
    min_value,
    max_value,
    
    model_description,
    reference_url
)
VALUES (
    'Adafruit VEML7700 Ambient Light Sensor',
    'SENSOR',
    'LIGHT',
    'Adafruit / Vishay',
    'lux',
    50.0,
    90000.0,
    'Logical model for ambient light intensity; mock range supports indoor greenhouse to strong daylight.',
    'https://www.adafruit.com/product/4162'
)
ON CONFLICT ((LOWER(model_name))) DO UPDATE SET
    device_type = EXCLUDED.device_type,
    metric_type = EXCLUDED.metric_type,
    manufacturer = EXCLUDED.manufacturer,
    display_unit = EXCLUDED.display_unit,
    min_value = EXCLUDED.min_value,
    max_value = EXCLUDED.max_value,
    
    model_description = EXCLUDED.model_description,
    reference_url = EXCLUDED.reference_url;

-- Adafruit TSL2591 High Range Light Sensor | unit: lux | mock range: 10.0 - 100000.0
INSERT INTO models (
    model_name,
    device_type,
    metric_type,
    manufacturer,
    display_unit,
    min_value,
    max_value,
    
    model_description,
    reference_url
)
VALUES (
    'Adafruit TSL2591 High Range Light Sensor',
    'SENSOR',
    'LIGHT',
    'Adafruit / AMS',
    'lux',
    10.0,
    100000.0,
    'Logical model for high dynamic range light telemetry.',
    'https://www.adafruit.com/product/1980'
)
ON CONFLICT ((LOWER(model_name))) DO UPDATE SET
    device_type = EXCLUDED.device_type,
    metric_type = EXCLUDED.metric_type,
    manufacturer = EXCLUDED.manufacturer,
    display_unit = EXCLUDED.display_unit,
    min_value = EXCLUDED.min_value,
    max_value = EXCLUDED.max_value,
    
    model_description = EXCLUDED.model_description,
    reference_url = EXCLUDED.reference_url;

-- Adafruit SCD-40 CO2 Sensor | unit: ppm | mock range: 400.0 - 2000.0
INSERT INTO models (
    model_name,
    device_type,
    metric_type,
    manufacturer,
    display_unit,
    min_value,
    max_value,
    
    model_description,
    reference_url
)
VALUES (
    'Adafruit SCD-40 CO2 Sensor',
    'SENSOR',
    'CO2',
    'Adafruit / Sensirion',
    'ppm',
    400.0,
    2000.0,
    'Logical model for CO2 concentration feed only; mock range fits greenhouse ventilation scenarios.',
    'https://www.adafruit.com/product/5187'
)
ON CONFLICT ((LOWER(model_name))) DO UPDATE SET
    device_type = EXCLUDED.device_type,
    metric_type = EXCLUDED.metric_type,
    manufacturer = EXCLUDED.manufacturer,
    display_unit = EXCLUDED.display_unit,
    min_value = EXCLUDED.min_value,
    max_value = EXCLUDED.max_value,
    
    model_description = EXCLUDED.model_description,
    reference_url = EXCLUDED.reference_url;

-- Adafruit Peristaltic Liquid Pump 12V | unit: ON_OFF | mock range: 0.0 - 1.0
INSERT INTO models (
    model_name,
    device_type,
    metric_type,
    manufacturer,
    display_unit,
    min_value,
    max_value,
    
    model_description,
    reference_url
)
VALUES (
    'Adafruit Peristaltic Liquid Pump 12V',
    'ACTUATOR',
    'PUMP',
    'Adafruit',
    'ON_OFF',
    0.0,
    1.0,
    'Logical pump actuator state; 0 means OFF, 1 means ON.',
    'https://www.adafruit.com/product/1150'
)
ON CONFLICT ((LOWER(model_name))) DO UPDATE SET
    device_type = EXCLUDED.device_type,
    metric_type = EXCLUDED.metric_type,
    manufacturer = EXCLUDED.manufacturer,
    display_unit = EXCLUDED.display_unit,
    min_value = EXCLUDED.min_value,
    max_value = EXCLUDED.max_value,
    
    model_description = EXCLUDED.model_description,
    reference_url = EXCLUDED.reference_url;

-- Adafruit Peristaltic Liquid Pump 5V | unit: ON_OFF | mock range: 0.0 - 1.0
INSERT INTO models (
    model_name,
    device_type,
    metric_type,
    manufacturer,
    display_unit,
    min_value,
    max_value,
    
    model_description,
    reference_url
)
VALUES (
    'Adafruit Peristaltic Liquid Pump 5V',
    'ACTUATOR',
    'PUMP',
    'Adafruit',
    'ON_OFF',
    0.0,
    1.0,
    'Logical small pump actuator state; suitable for nutrient dosing or small irrigation mock control.',
    'https://www.adafruit.com/product/3910'
)
ON CONFLICT ((LOWER(model_name))) DO UPDATE SET
    device_type = EXCLUDED.device_type,
    metric_type = EXCLUDED.metric_type,
    manufacturer = EXCLUDED.manufacturer,
    display_unit = EXCLUDED.display_unit,
    min_value = EXCLUDED.min_value,
    max_value = EXCLUDED.max_value,
    
    model_description = EXCLUDED.model_description,
    reference_url = EXCLUDED.reference_url;

-- Adafruit Submersible Water Pump 3V | unit: ON_OFF | mock range: 0.0 - 1.0
INSERT INTO models (
    model_name,
    device_type,
    metric_type,
    manufacturer,
    display_unit,
    min_value,
    max_value,
    
    model_description,
    reference_url
)
VALUES (
    'Adafruit Submersible Water Pump 3V',
    'ACTUATOR',
    'PUMP',
    'Adafruit',
    'ON_OFF',
    0.0,
    1.0,
    'Logical submersible pump state; 0 means OFF, 1 means ON.',
    'https://www.adafruit.com/product/4546'
)
ON CONFLICT ((LOWER(model_name))) DO UPDATE SET
    device_type = EXCLUDED.device_type,
    metric_type = EXCLUDED.metric_type,
    manufacturer = EXCLUDED.manufacturer,
    display_unit = EXCLUDED.display_unit,
    min_value = EXCLUDED.min_value,
    max_value = EXCLUDED.max_value,
    
    model_description = EXCLUDED.model_description,
    reference_url = EXCLUDED.reference_url;

-- Adafruit Brass Liquid Solenoid Valve 12V | unit: OPEN_CLOSED | mock range: 0.0 - 1.0
INSERT INTO models (
    model_name,
    device_type,
    metric_type,
    manufacturer,
    display_unit,
    min_value,
    max_value,
    
    model_description,
    reference_url
)
VALUES (
    'Adafruit Brass Liquid Solenoid Valve 12V',
    'ACTUATOR',
    'VALVE',
    'Adafruit',
    'OPEN_CLOSED',
    0.0,
    1.0,
    'Logical irrigation valve state; 0 means CLOSED, 1 means OPEN.',
    'https://www.adafruit.com/product/996'
)
ON CONFLICT ((LOWER(model_name))) DO UPDATE SET
    device_type = EXCLUDED.device_type,
    metric_type = EXCLUDED.metric_type,
    manufacturer = EXCLUDED.manufacturer,
    display_unit = EXCLUDED.display_unit,
    min_value = EXCLUDED.min_value,
    max_value = EXCLUDED.max_value,
    
    model_description = EXCLUDED.model_description,
    reference_url = EXCLUDED.reference_url;

-- Adafruit Plastic Water Solenoid Valve 12V | unit: OPEN_CLOSED | mock range: 0.0 - 1.0
INSERT INTO models (
    model_name,
    device_type,
    metric_type,
    manufacturer,
    display_unit,
    min_value,
    max_value,
    
    model_description,
    reference_url
)
VALUES (
    'Adafruit Plastic Water Solenoid Valve 12V',
    'ACTUATOR',
    'VALVE',
    'Adafruit',
    'OPEN_CLOSED',
    0.0,
    1.0,
    'Logical water valve state for irrigation control; 0 means CLOSED, 1 means OPEN.',
    'https://www.adafruit.com/product/997'
)
ON CONFLICT ((LOWER(model_name))) DO UPDATE SET
    device_type = EXCLUDED.device_type,
    metric_type = EXCLUDED.metric_type,
    manufacturer = EXCLUDED.manufacturer,
    display_unit = EXCLUDED.display_unit,
    min_value = EXCLUDED.min_value,
    max_value = EXCLUDED.max_value,
    
    model_description = EXCLUDED.model_description,
    reference_url = EXCLUDED.reference_url;

-- Adafruit Power Relay FeatherWing | unit: ON_OFF | mock range: 0.0 - 1.0
INSERT INTO models (
    model_name,
    device_type,
    metric_type,
    manufacturer,
    display_unit,
    min_value,
    max_value,
    
    model_description,
    reference_url
)
VALUES (
    'Adafruit Power Relay FeatherWing',
    'ACTUATOR',
    'RELAY',
    'Adafruit',
    'ON_OFF',
    0.0,
    1.0,
    'Logical relay state for controlling external loads; 0 means OFF, 1 means ON.',
    'https://www.adafruit.com/product/3191'
)
ON CONFLICT ((LOWER(model_name))) DO UPDATE SET
    device_type = EXCLUDED.device_type,
    metric_type = EXCLUDED.metric_type,
    manufacturer = EXCLUDED.manufacturer,
    display_unit = EXCLUDED.display_unit,
    min_value = EXCLUDED.min_value,
    max_value = EXCLUDED.max_value,
    
    model_description = EXCLUDED.model_description,
    reference_url = EXCLUDED.reference_url;

-- Adafruit STEMMA MOSFET Driver | unit: ON_OFF | mock range: 0.0 - 1.0
INSERT INTO models (
    model_name,
    device_type,
    metric_type,
    manufacturer,
    display_unit,
    min_value,
    max_value,
    
    model_description,
    reference_url
)
VALUES (
    'Adafruit STEMMA MOSFET Driver',
    'ACTUATOR',
    'RELAY',
    'Adafruit',
    'ON_OFF',
    0.0,
    1.0,
    'Logical MOSFET switch state for solenoids, pumps, LEDs, or fans; 0 means OFF, 1 means ON.',
    'https://www.adafruit.com/product/5648'
)
ON CONFLICT ((LOWER(model_name))) DO UPDATE SET
    device_type = EXCLUDED.device_type,
    metric_type = EXCLUDED.metric_type,
    manufacturer = EXCLUDED.manufacturer,
    display_unit = EXCLUDED.display_unit,
    min_value = EXCLUDED.min_value,
    max_value = EXCLUDED.max_value,
    
    model_description = EXCLUDED.model_description,
    reference_url = EXCLUDED.reference_url;

-- Adafruit Miniature 5V Cooling Fan | unit: ON_OFF | mock range: 0.0 - 1.0
INSERT INTO models (
    model_name,
    device_type,
    metric_type,
    manufacturer,
    display_unit,
    min_value,
    max_value,
    
    model_description,
    reference_url
)
VALUES (
    'Adafruit Miniature 5V Cooling Fan',
    'ACTUATOR',
    'FAN',
    'Adafruit',
    'ON_OFF',
    0.0,
    1.0,
    'Logical fan state for greenhouse ventilation; 0 means OFF, 1 means ON.',
    'https://www.adafruit.com/product/3368'
)
ON CONFLICT ((LOWER(model_name))) DO UPDATE SET
    device_type = EXCLUDED.device_type,
    metric_type = EXCLUDED.metric_type,
    manufacturer = EXCLUDED.manufacturer,
    display_unit = EXCLUDED.display_unit,
    min_value = EXCLUDED.min_value,
    max_value = EXCLUDED.max_value,
    
    model_description = EXCLUDED.model_description,
    reference_url = EXCLUDED.reference_url;

COMMIT;
