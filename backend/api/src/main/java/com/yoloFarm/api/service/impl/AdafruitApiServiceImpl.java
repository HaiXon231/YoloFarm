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
            log.info("AdafruitAPI: Sending request to create feed [key={}, name={}]...", feedKey, feedName);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
            log.info("AdafruitAPI: Feed created successfully. Status: {}", response.getStatusCode());
        } catch (HttpClientErrorException e) {
            String errorMsg = e.getResponseBodyAsString();
            log.error("AdafruitAPI: Failed to create feed: status={}, response={}", e.getStatusCode(), errorMsg);

            if (e.getStatusCode().value() == 422) {
                // Adafruit returns 422 if feed already exists or invalid format
                if (errorMsg.contains("has already been taken")) {
                    log.warn("AdafruitAPI: Feed '{}' already exists, skipping creation.", feedKey);
                } else {
                    throw new IllegalStateException("Cannot create Adafruit feed due to invalid format (422): " + errorMsg);
                }
            } else if (e.getStatusCode().value() == 403 || e.getStatusCode().value() == 401) {
                throw new IllegalStateException(
                        "Adafruit auth error (403). You may have reached the 10-feed free limit. Details: "
                                + errorMsg);
            } else {
                throw new IllegalStateException("Adafruit server error (" + e.getStatusCode() + "): " + errorMsg);
            }
        } catch (Exception e) {
            log.error("AdafruitAPI: Internal error connecting to Adafruit REST API", e);
            throw new IllegalStateException("HTTP connection error to Adafruit: " + e.getMessage());
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
            log.info("AdafruitAPI: Sending request to rename feed [key={}, newName={}]...", feedKey, feedName);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PUT, request, String.class);
            log.info("AdafruitAPI: Feed renamed successfully. Status: {}", response.getStatusCode());
        } catch (HttpClientErrorException e) {
            String errorMsg = e.getResponseBodyAsString();
            log.error("AdafruitAPI: Failed to rename feed: status={}, response={}", e.getStatusCode(), errorMsg);

            if (e.getStatusCode().value() == 404) {
                throw new IllegalStateException("Feed not found on Adafruit for rename: " + feedKey);
            } else if (e.getStatusCode().value() == 422) {
                throw new IllegalStateException(
                        "Cannot rename Adafruit feed due to invalid data (422): " + errorMsg);
            } else if (e.getStatusCode().value() == 403 || e.getStatusCode().value() == 401) {
                throw new IllegalStateException(
                        "Adafruit auth error when renaming feed (403/401). Details: " + errorMsg);
            } else {
                throw new IllegalStateException(
                        "Adafruit server error when renaming feed (" + e.getStatusCode() + "): " + errorMsg);
            }
        } catch (Exception e) {
            log.error("AdafruitAPI: Internal error calling rename feed API", e);
            throw new IllegalStateException("HTTP connection error to Adafruit: " + e.getMessage());
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
            log.info("AdafruitAPI: Sending request to delete feed [key={}]...", feedKey);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.DELETE, request, String.class);
            log.info("AdafruitAPI: Feed deleted successfully. Status: {}", response.getStatusCode());
        } catch (HttpClientErrorException e) {
            String errorMsg = e.getResponseBodyAsString();
            log.error("AdafruitAPI: Failed to delete feed: status={}, response={}", e.getStatusCode(), errorMsg);

            if (e.getStatusCode().value() == 404) {
                log.warn("AdafruitAPI: Feed [{}] not found on Adafruit, skipping remote deletion.", feedKey);
                return;
            }
            if (e.getStatusCode().value() == 403 || e.getStatusCode().value() == 401) {
                throw new IllegalStateException(
                        "Adafruit auth error when deleting feed (403/401). Details: " + errorMsg);
            }

            throw new IllegalStateException(
                    "Adafruit server error when deleting feed (" + e.getStatusCode() + "): " + errorMsg);
        } catch (Exception e) {
            log.error("AdafruitAPI: Internal error calling delete feed API", e);
            throw new IllegalStateException("HTTP connection error to Adafruit: " + e.getMessage());
        }
    }
}
