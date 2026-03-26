package com.yoloFarm.api;

import com.yoloFarm.api.dto.response.DeviceResponse;
import com.yoloFarm.api.dto.response.FarmResponse;
import com.yoloFarm.api.dto.response.LoginResponse;
import com.yoloFarm.api.dto.response.NotificationResponse;
import com.yoloFarm.api.enums.ConnectionStatusEnum;
import com.yoloFarm.api.enums.DeviceStatusEnum;
import com.yoloFarm.api.enums.OperatingModeEnum;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.eclipse.paho.client.mqttv3.IMqttClient;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class ApiContractSerializationTest {

    @MockitoBean
    private IMqttClient mqttClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void loginResponseShouldSerializeSnakeCase() throws Exception {
        LoginResponse response = new LoginResponse();
        response.setAccessToken("token");
        response.setTokenType("Bearer");
        response.setExpiresIn(86400);
        response.setRole("FARMER");

        String json = objectMapper.writeValueAsString(response);

        assertTrue(json.contains("\"access_token\""));
        assertTrue(json.contains("\"token_type\""));
        assertTrue(json.contains("\"expires_in\""));
        assertFalse(json.contains("\"accessToken\""));
        assertFalse(json.contains("\"tokenType\""));
        assertFalse(json.contains("\"expiresIn\""));
    }

    @Test
    void farmAndDeviceResponseShouldSerializeSnakeCase() throws Exception {
        FarmResponse farm = new FarmResponse();
        farm.setId(UUID.randomUUID());
        farm.setOwnerId(UUID.randomUUID());
        farm.setName("Farm A");
        farm.setLocation("Zone 1");
        farm.setCreatedAt(LocalDateTime.now());

        DeviceResponse device = new DeviceResponse();
        device.setId(UUID.randomUUID());
        device.setFarmId(farm.getId());
        device.setModelId(UUID.randomUUID());
        device.setName("Pump 01");
        device.setStatus(DeviceStatusEnum.ACTIVE);
        device.setAdafruitFeedKey("pump-01");
        device.setConnectionStatus(ConnectionStatusEnum.ONLINE);
        device.setLastSeen(LocalDateTime.now());
        device.setOperatingMode(OperatingModeEnum.AUTO);
        device.setIsActive(true);

        String farmJson = objectMapper.writeValueAsString(farm);
        String deviceJson = objectMapper.writeValueAsString(device);

        assertTrue(farmJson.contains("\"owner_id\""));
        assertTrue(farmJson.contains("\"created_at\""));
        assertFalse(farmJson.contains("\"ownerId\""));
        assertFalse(farmJson.contains("\"createdAt\""));

        assertTrue(deviceJson.contains("\"farm_id\""));
        assertTrue(deviceJson.contains("\"model_id\""));
        assertTrue(deviceJson.contains("\"adafruit_feed_key\""));
        assertTrue(deviceJson.contains("\"connection_status\""));
        assertTrue(deviceJson.contains("\"last_seen\""));
        assertTrue(deviceJson.contains("\"operating_mode\""));
        assertTrue(deviceJson.contains("\"is_active\""));
        assertFalse(deviceJson.contains("\"farmId\""));
        assertFalse(deviceJson.contains("\"adafruitFeedKey\""));
    }

    @Test
    void notificationResponseShouldSerializeSnakeCase() throws Exception {
        NotificationResponse response = new NotificationResponse();
        response.setId(UUID.randomUUID());
        response.setMessage("ok");
        response.setIsRead(true);
        response.setCreatedAt(LocalDateTime.now());

        String json = objectMapper.writeValueAsString(response);

        assertTrue(json.contains("\"is_read\""));
        assertTrue(json.contains("\"created_at\""));
        assertFalse(json.contains("\"isRead\""));
        assertFalse(json.contains("\"createdAt\""));
    }
}
