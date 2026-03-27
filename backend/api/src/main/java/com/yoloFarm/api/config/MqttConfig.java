package com.yoloFarm.api.config;

import com.yoloFarm.api.service.mqtt.MqttReceiverService;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.ObjectProvider;
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

    private final ObjectProvider<IMqttClient> mqttClientProvider;
    private final ObjectProvider<MqttReceiverService> mqttReceiverServiceProvider;

    public MqttConfig(
            ObjectProvider<IMqttClient> mqttClientProvider,
            ObjectProvider<MqttReceiverService> mqttReceiverServiceProvider) {
        this.mqttClientProvider = mqttClientProvider;
        this.mqttReceiverServiceProvider = mqttReceiverServiceProvider;
    }

    @Bean(destroyMethod = "disconnectForcibly")
    public IMqttClient mqttClient() throws MqttException {
        return new MqttClient(brokerUrl, clientId, new MemoryPersistence());
    }

    @EventListener(ApplicationReadyEvent.class)
    public void connectMqtt() {
        try {
            IMqttClient mqttClient = mqttClientProvider.getIfAvailable();
            if (mqttClient == null || mqttClient.isConnected()) {
                return;
            }

            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName(username);
            options.setPassword(password.toCharArray());
            options.setCleanSession(true);
            options.setAutomaticReconnect(true); // Tự kết nối lại khi mất kết nối

            mqttClient.connect(options);
            log.info("MqttConfig: Kết nối thành công tới Adafruit IO MQTT Broker!");

            MqttReceiverService receiverService = mqttReceiverServiceProvider.getIfAvailable();
            if (receiverService != null) {
                receiverService.subscribeIfConnected();
            }
        } catch (MqttException e) {
            // App vẫn chạy bình thường, MQTT sẽ tự reconnect sau
            log.error("MqttConfig: Không thể kết nối MQTT Broker. Sẽ thử kết nối lại tự động.", e);
        }
    }
}
