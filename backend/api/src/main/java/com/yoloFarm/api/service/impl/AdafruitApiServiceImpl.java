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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
                throw new IllegalStateException(
                        "Lỗi xác thực Adafruit (403). Bạn có thể đã tải hết giới hạn 10 Feed miễn phí. Chi tiết: "
                                + errorMsg);
            } else {
                throw new IllegalStateException("Lỗi từ máy chủ Adafruit (" + e.getStatusCode() + "): " + errorMsg);
            }
        } catch (Exception e) {
            log.error("Lỗi nội bộ khi kết nối Adafruit REST API", e);
            throw new IllegalStateException("Lỗi sự cố khi kết nối HTTP tới Adafruit: " + e.getMessage());
        }
    }

    @Override
    public void updateFeedName(String feedKey, String feedName) {
        String encodedFeedKey = URLEncoder.encode(feedKey, StandardCharsets.UTF_8).replace("+", "%20");
        String url = "https://io.adafruit.com/api/v2/" + username + "/feeds/" + encodedFeedKey;

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-AIO-Key", aioKey);
        headers.set("Content-Type", "application/json");

        Map<String, Object> body = new HashMap<>();
        body.put("name", feedName);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            log.info("Gửi request đổi tên Feed [key={}, newName={}] lên Adafruit IO...", feedKey, feedName);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PUT, request, String.class);
            log.info("Đổi tên Feed thành công trên Adafruit. Status: {}", response.getStatusCode());
        } catch (HttpClientErrorException e) {
            String errorMsg = e.getResponseBodyAsString();
            log.error("Lỗi khi đổi tên Adafruit Feed: Mã lỗi = {}, Response = {}", e.getStatusCode(), errorMsg);

            if (e.getStatusCode().value() == 404) {
                throw new IllegalStateException("Không tìm thấy Feed trên Adafruit để đổi tên: " + feedKey);
            } else if (e.getStatusCode().value() == 422) {
                throw new IllegalStateException(
                        "Không thể đổi tên Feed Adafruit do dữ liệu không hợp lệ (422): " + errorMsg);
            } else if (e.getStatusCode().value() == 403 || e.getStatusCode().value() == 401) {
                throw new IllegalStateException(
                        "Lỗi xác thực Adafruit khi đổi tên Feed (403/401). Chi tiết: " + errorMsg);
            } else {
                throw new IllegalStateException(
                        "Lỗi từ máy chủ Adafruit khi đổi tên Feed (" + e.getStatusCode() + "): " + errorMsg);
            }
        } catch (Exception e) {
            log.error("Lỗi nội bộ khi gọi API đổi tên Feed Adafruit", e);
            throw new IllegalStateException("Lỗi sự cố khi kết nối HTTP tới Adafruit: " + e.getMessage());
        }
    }

    @Override
    public void deleteFeed(String feedKey) {
        String encodedFeedKey = URLEncoder.encode(feedKey, StandardCharsets.UTF_8).replace("+", "%20");
        String url = "https://io.adafruit.com/api/v2/" + username + "/feeds/" + encodedFeedKey;

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-AIO-Key", aioKey);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            log.info("Gửi request xóa Feed [key={}] trên Adafruit IO...", feedKey);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.DELETE, request, String.class);
            log.info("Xóa Feed thành công trên Adafruit. Status: {}", response.getStatusCode());
        } catch (HttpClientErrorException e) {
            String errorMsg = e.getResponseBodyAsString();
            log.error("Lỗi khi xóa Adafruit Feed: Mã lỗi = {}, Response = {}", e.getStatusCode(), errorMsg);

            if (e.getStatusCode().value() == 404) {
                log.warn("Feed [{}] không tồn tại trên Adafruit, bỏ qua xóa feed từ xa.", feedKey);
                return;
            }
            if (e.getStatusCode().value() == 403 || e.getStatusCode().value() == 401) {
                throw new IllegalStateException(
                        "Lỗi xác thực Adafruit khi xóa Feed (403/401). Chi tiết: " + errorMsg);
            }

            throw new IllegalStateException(
                    "Lỗi từ máy chủ Adafruit khi xóa Feed (" + e.getStatusCode() + "): " + errorMsg);
        } catch (Exception e) {
            log.error("Lỗi nội bộ khi gọi API xóa Feed Adafruit", e);
            throw new IllegalStateException("Lỗi sự cố khi kết nối HTTP tới Adafruit: " + e.getMessage());
        }
    }
}
