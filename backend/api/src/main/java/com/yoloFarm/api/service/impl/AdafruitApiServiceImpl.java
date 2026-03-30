package com.yoloFarm.api.service.impl;

import com.yoloFarm.api.service.AdafruitApiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class AdafruitApiServiceImpl implements AdafruitApiService {

    private final RestTemplate restTemplate;

    @Value("${adafruit.mqtt.username}")
    private String username;

    @Value("${adafruit.mqtt.password}")
    private String aioKey;

    public AdafruitApiServiceImpl() {
        this.restTemplate = new RestTemplate();
    }

    @Override
    public void createFeed(String feedKey, String feedName) {
        String url = "https://io.adafruit.com/api/v2/" + username + "/feeds";

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-AIO-Key", aioKey);
        headers.set("Content-Type", "application/json");

        Map<String, Object> feed = new HashMap<>();
        feed.put("name", feedName);
        feed.put("key", feedKey);

        Map<String, Object> body = new HashMap<>();
        body.put("feed", feed);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            log.info("Gửi request tạo Feed [key={}, name={}] lên Adafruit IO...", feedKey, feedName);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
            log.info("Feed được tạo thành công trên Adafruit. Status: {}", response.getStatusCode());
        } catch (HttpClientErrorException e) {
            String errorMsg = e.getResponseBodyAsString();
            log.error("Lỗi khi tạo Adafruit Feed: Mã lỗi = {}, Response = {}", e.getStatusCode(), errorMsg);
            
            if (e.getStatusCode().value() == 422) {
                // Adafruit returns 422 if feed already exists or invalid format
                if (errorMsg.contains("has already been taken")) {
                    log.warn("Feed '{}' đã tồn tại trên Adafruit, bỏ qua bước khởi tạo.", feedKey);
                } else {
                    throw new IllegalStateException("Không thể tạo Feed Adafruit do sai định dạng (422): " + errorMsg);
                }
            } else if (e.getStatusCode().value() == 403 || e.getStatusCode().value() == 401) {
                throw new IllegalStateException("Lỗi xác thực Adafruit (403). Bạn có thể đã tải hết giới hạn 10 Feed miễn phí. Chi tiết: " + errorMsg);
            } else {
                throw new IllegalStateException("Lỗi từ máy chủ Adafruit (" + e.getStatusCode() + "): " + errorMsg);
            }
        } catch (Exception e) {
            log.error("Lỗi nội bộ khi kết nối Adafruit REST API", e);
            throw new IllegalStateException("Lỗi sự cố khi kết nối HTTP tới Adafruit: " + e.getMessage());
        }
    }
}
