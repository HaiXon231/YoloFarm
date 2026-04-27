# Plan 04-02: Backend Docker & HuggingFace Deploy

## What was built
- Tạo `Dockerfile` dạng multi-stage (Maven build -> JRE run) sử dụng `eclipse-temurin:21` tương thích với Java version của codebase.
- Định tuyến port thành `7860` cho Spring Boot thông qua biến môi trường mặc định `SERVER_PORT=7860`, giúp container tự động nghe cổng phù hợp với yêu cầu của HuggingFace Spaces.
- Tạo `.dockerignore` bỏ qua `target/`, `.env`... để tăng tốc độ build trên cloud và bảo vệ secrets.
- Cấu hình Global CORS (`CorsConfig.java`) và cho phép `.cors()` trong `SecurityConfig.java` để đảm bảo Vercel có thể kết nối được tới backend không bị lỗi CORS.

## Verification
- Backend tests passed thành công (53 tests). CORS logic không làm ảnh hưởng tới các security rules cũ.

## Next Steps
- Tạo HuggingFace Space (Docker space).
- Điền toàn bộ các Biến môi trường (Secrets) như: `DB_USERNAME`, `DB_PASSWORD`, `DB_URL`, `JWT_SECRET_KEY`, `ADAFRUIT_USERNAME`, `ADAFRUIT_IO_KEY`, `WS_ALLOWED_ORIGINS`.
