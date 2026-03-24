package com.yoloFarm.api.service.impl;

import com.yoloFarm.api.service.mqtt.MqttSenderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MqttSenderServiceImpl implements MqttSenderService {

    @Override
    public void sendCommand(String adafruitFeedKey, String command) {
        log.info("Hệ thống giả lập: Đang gửi lệnh {} đến Adafruit Feed Key: {}", command, adafruitFeedKey);
    }
}
