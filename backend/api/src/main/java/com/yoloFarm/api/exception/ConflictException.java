package com.yoloFarm.api.exception;

/**
 * Exception cho trường hợp tài nguyên bị trùng lặp (HTTP 409 Conflict).
 * Ví dụ: Username hoặc Email đã tồn tại khi đăng ký.
 */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
