package com.yoloFarm.api;

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class ApiApplicationTests {

    @MockitoBean
    private IMqttClient mqttClient;

    @Test
    void contextLoads() {
    }

}
