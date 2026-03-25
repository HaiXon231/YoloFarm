package com.yoloFarm.api.config;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

@Configuration
@Slf4j
public class MqttConfig {

    @Value("${adafruit.mqtt.broker-url}")
    private String brokerUrl;

    @Value("${adafruit.mqtt.client-id}")
    private String clientId;

    @Value("${adafruit.mqtt.username}")
    private String username;

    @Value("${adafruit.mqtt.password}")
    private String password;

    private IMqttClient mqttClient;

    @Bean
    public IMqttClient mqttClient() throws MqttException {
        // Chỉ tạo client, KHÔNG connect() ở đây
        // → App vẫn start được dù broker offline
        mqttClient = new MqttClient(brokerUrl, clientId);
        return mqttClient;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void connectMqtt() {
        try {
            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName(username);
            options.setPassword(password.toCharArray());
            options.setCleanSession(true);
            options.setAutomaticReconnect(true);  // Tự kết nối lại khi mất kết nối

            mqttClient.connect(options);
            log.info("MqttConfig: Kết nối thành công tới Adafruit IO MQTT Broker!");
        } catch (MqttException e) {
            // App vẫn chạy bình thường, MQTT sẽ tự reconnect sau
            log.error("MqttConfig: Không thể kết nối MQTT Broker. Sẽ thử kết nối lại tự động.", e);
        }
    }
}
