package com.yoloFarm.api.service;

public interface AdafruitApiService {
    /**
     * Tự động tạo một Feed mới trên Adafruit IO sử dụng REST API (v2).
     * 
     * @param feedKey  Mã khóa (key) của Feed (không khoảng trắng, không ký tự đặc biệt)
     * @param feedName Tên hiển thị của Feed trên Dashboard
     * @throws IllegalStateException Nếu gọi API lên Adafruit thất bại (vd: quá hạn ngạch 10 feeds, 403, 422)
     */
    void createFeed(String feedKey, String feedName);
}
