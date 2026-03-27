-- Seed sample device models for quick manual testing in DBeaver.
-- Safe to run multiple times (uses WHERE NOT EXISTS).

INSERT INTO models (id, model_name, device_type, metric_type, manufacturer)
SELECT '11111111-1111-1111-1111-111111111111'::uuid, 'Temp Sensor v1', 'SENSOR', 'TEMP', 'YoloFarm'
WHERE NOT EXISTS (
    SELECT 1 FROM models WHERE model_name = 'Temp Sensor v1' AND metric_type = 'TEMP'
);

INSERT INTO models (id, model_name, device_type, metric_type, manufacturer)
SELECT '22222222-2222-2222-2222-222222222222'::uuid, 'Humidity Sensor v1', 'SENSOR', 'HUMIDITY', 'YoloFarm'
WHERE NOT EXISTS (
    SELECT 1 FROM models WHERE model_name = 'Humidity Sensor v1' AND metric_type = 'HUMIDITY'
);

INSERT INTO models (id, model_name, device_type, metric_type, manufacturer)
SELECT '33333333-3333-3333-3333-333333333333'::uuid, 'Soil Sensor v1', 'SENSOR', 'SOIL_MOISTURE', 'YoloFarm'
WHERE NOT EXISTS (
    SELECT 1 FROM models WHERE model_name = 'Soil Sensor v1' AND metric_type = 'SOIL_MOISTURE'
);

INSERT INTO models (id, model_name, device_type, metric_type, manufacturer)
SELECT '44444444-4444-4444-4444-444444444444'::uuid, 'Light Sensor v1', 'SENSOR', 'LIGHT', 'YoloFarm'
WHERE NOT EXISTS (
    SELECT 1 FROM models WHERE model_name = 'Light Sensor v1' AND metric_type = 'LIGHT'
);

INSERT INTO models (id, model_name, device_type, metric_type, manufacturer)
SELECT '55555555-5555-5555-5555-555555555555'::uuid, 'Pump Controller v1', 'ACTUATOR', 'PUMP', 'YoloFarm'
WHERE NOT EXISTS (
    SELECT 1 FROM models WHERE model_name = 'Pump Controller v1' AND metric_type = 'PUMP'
);
