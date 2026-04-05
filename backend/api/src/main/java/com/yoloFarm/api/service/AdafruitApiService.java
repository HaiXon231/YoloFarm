package com.yoloFarm.api.service;

public interface AdafruitApiService {
    /**
     * Tự động tạo một Feed mới trên Adafruit IO sử dụng REST API (v2).
     * 
     * @param feedKey  Mã khóa (key) của Feed (không khoảng trắng, không ký tự đặc
     *                 biệt)
     * @param feedName Tên hiển thị của Feed trên Dashboard
     * @throws IllegalStateException Nếu gọi API lên Adafruit thất bại (vd: quá hạn
     *                               ngạch 10 feeds, 403, 422)
     */
    void createFeed(String feedKey, String feedName);

    /**
     * Cập nhật tên hiển thị của Feed đã tồn tại trên Adafruit IO.
     *
     * @param feedKey  Mã khóa kỹ thuật của Feed
     * @param feedName Tên hiển thị mới của Feed
     */
    void updateFeedName(String feedKey, String feedName);

    /**
     * Xóa Feed khỏi Adafruit IO theo feed key.
     *
     * @param feedKey Mã khóa kỹ thuật của Feed
     */
    void deleteFeed(String feedKey);
}
