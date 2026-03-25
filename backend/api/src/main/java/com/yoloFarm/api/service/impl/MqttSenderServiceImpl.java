package com.yoloFarm.api.service.impl;

import com.yoloFarm.api.service.mqtt.MqttSenderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class MqttSenderServiceImpl implements MqttSenderService {

    private final IMqttClient mqttClient;

    @Value("${adafruit.mqtt.username}")
    private String username;

    @Override
    public void sendCommand(String adafruitFeedKey, String command) {
        try {
            String topic = username + "/feeds/" + adafruitFeedKey;
            
            MqttMessage message = new MqttMessage(command.getBytes());
            message.setQos(1); // Mức Quality of Service 1: Đảm bảo ít nhất message đến được đích
            
            mqttClient.publish(topic, message);
            log.info("MqttSender: Đã pub lệnh điều khiển [{}] xuống topic [{}]", command, topic);
        } catch (MqttException e) {
            // Ném exception để caller biết lệnh chưa gửi được → trả HTTP 500/502
            throw new RuntimeException("Không thể gửi lệnh MQTT tới feed: " + adafruitFeedKey, e);
        }
    }
}
