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

    @Bean
    public IMqttClient mqttClient() throws MqttException {
        // Chỉ tạo client instance, KHÔNG connect() ở đây
        // → nếu Broker offline, app vẫn start được
        return new MqttClient(brokerUrl, clientId);
    }

    /**
     * Kết nối MQTT sau khi Spring Boot đã khởi động hoàn tất.
     * Nếu broker offline → log error nhưng app vẫn chạy.
     * AutoReconnect = true → tự kết nối lại khi broker online.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void connectToBroker() {
        try {
            IMqttClient client = mqttClient();
            if (!client.isConnected()) {
                MqttConnectOptions options = new MqttConnectOptions();
                options.setAutomaticReconnect(true);
                options.setCleanSession(true);
                options.setUserName(username);
                options.setPassword(password.toCharArray());
                options.setConnectionTimeout(10);
                client.connect(options);
                log.info("MqttConfig: Đã kết nối thành công tới broker: {}", brokerUrl);
            }
        } catch (MqttException e) {
            log.error("MqttConfig: Không thể kết nối tới broker {}. App vẫn chạy, sẽ tự reconnect.", brokerUrl, e);
        }
    }
}
