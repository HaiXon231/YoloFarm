package com.yoloFarm.api.service.mqtt;

public interface MqttSenderService {
    void sendCommand(String adafruitFeedKey, String command);
}
