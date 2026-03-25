package com.yoloFarm.api.exception;

/**
 * Exception cho trường hợp tài nguyên bị trùng lặp (HTTP 409 Conflict).
 * Ví dụ: đăng ký username/email đã tồn tại.
 */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
