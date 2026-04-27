package com.yoloFarm.api.exception;

import com.yoloFarm.api.dto.response.ErrorResponse;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

        @ExceptionHandler(EntityNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleEntityNotFoundException(EntityNotFoundException ex) {
                return build(HttpStatus.NOT_FOUND, "Không tìm thấy tài nguyên", ex.getMessage());
        }

        @ExceptionHandler(ConflictException.class)
        public ResponseEntity<ErrorResponse> handleConflictException(ConflictException ex) {
                return build(HttpStatus.CONFLICT, "Xung đột dữ liệu", ex.getMessage());
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
                String details = ex.getBindingResult().getFieldErrors().stream()
                                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                                .collect(Collectors.joining(", "));
                return build(HttpStatus.BAD_REQUEST, "Dữ liệu không hợp lệ", details);
        }

        @ExceptionHandler(ConstraintViolationException.class)
        public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
                return build(HttpStatus.BAD_REQUEST, "Dữ liệu không hợp lệ", ex.getMessage());
        }

        @ExceptionHandler(BadCredentialsException.class)
        public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
                return build(HttpStatus.UNAUTHORIZED, "Sai tên đăng nhập hoặc mật khẩu", ex.getMessage());
        }

        @ExceptionHandler(AuthenticationException.class)
        public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex) {
                return build(HttpStatus.UNAUTHORIZED, "Bạn chưa được xác thực hoặc phiên đăng nhập đã hết hạn",
                                ex.getMessage());
        }

        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
                return build(HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập tài nguyên này", ex.getMessage());
        }

        @ExceptionHandler(IllegalStateException.class)
        public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
                return build(HttpStatus.BAD_REQUEST, "Trạng thái không hợp lệ", ex.getMessage());
        }

        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
                return build(HttpStatus.BAD_REQUEST, "Dữ liệu đầu vào không hợp lệ", ex.getMessage());
        }

        @ExceptionHandler(HttpMessageNotReadableException.class)
        public ResponseEntity<ErrorResponse> handleUnreadableJson(HttpMessageNotReadableException ex) {
                return build(HttpStatus.BAD_REQUEST, "Body JSON không hợp lệ hoặc đang rỗng",
                                ex.getMostSpecificCause().getMessage());
        }

        @ExceptionHandler(DataIntegrityViolationException.class)
        public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
                return build(HttpStatus.CONFLICT,
                                "Xung đột dữ liệu",
                                "Kiểm tra dữ liệu trùng lặp hoặc ràng buộc khóa duy nhất");
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
                log.error("Unknown system error", ex);
                return build(HttpStatus.INTERNAL_SERVER_ERROR,
                                "Đã xảy ra lỗi hệ thống. Vui lòng thử lại sau.",
                                null);
        }

        private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, String details) {
                ErrorResponse response = new ErrorResponse();
                response.setCode(status.value());
                response.setMessage(message);
                response.setDetails(details);
                return ResponseEntity.status(status).body(response);
        }
}
