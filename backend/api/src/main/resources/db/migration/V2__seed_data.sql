-- ============================================================
-- V2: Seed data — Admin account + Device models
-- ============================================================

-- Admin account
-- Password: Admin@1234 (BCrypt)
INSERT INTO users (id, username, email, password, role, created_at)
VALUES (
    'a0000000-0000-0000-0000-000000000001',
    'admin',
    'admin@yolofarm.com',
    '$2a$10$.D6je4Mkhh//BGNxOpCsOeIpF5nzgwslvZEJw8rGBHyaW1jc.VbCS',
    'ADMIN',
    NOW()
);

-- ============================================================
-- Device models (SENSOR)
-- ============================================================

-- Cảm biến nhiệt độ
INSERT INTO models (id, model_name, manufacturer, device_type, metric_type)
VALUES (
    'b0000000-0000-0000-0000-000000000001',
    'Temperature Sensor',
    'YoloFarm',
    'SENSOR',
    'TEMP'
);

-- Cảm biến độ ẩm không khí
INSERT INTO models (id, model_name, manufacturer, device_type, metric_type)
VALUES (
    'b0000000-0000-0000-0000-000000000002',
    'Humidity Sensor',
    'YoloFarm',
    'SENSOR',
    'HUMIDITY'
);

-- Cảm biến độ ẩm đất
INSERT INTO models (id, model_name, manufacturer, device_type, metric_type)
VALUES (
    'b0000000-0000-0000-0000-000000000003',
    'Soil Moisture Sensor',
    'YoloFarm',
    'SENSOR',
    'SOIL_MOISTURE'
);

-- Cảm biến ánh sáng
INSERT INTO models (id, model_name, manufacturer, device_type, metric_type)
VALUES (
    'b0000000-0000-0000-0000-000000000004',
    'Light Sensor',
    'YoloFarm',
    'SENSOR',
    'LIGHT'
);

-- ============================================================
-- Device models (ACTUATOR)
-- ============================================================

-- Máy bơm nước
INSERT INTO models (id, model_name, manufacturer, device_type, metric_type)
VALUES (
    'b0000000-0000-0000-0000-000000000005',
    'Water Pump',
    'YoloFarm',
    'ACTUATOR',
    'PUMP'
);
