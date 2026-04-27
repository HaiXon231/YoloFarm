# Plan 04-01: Database Deployment (Neon.tech)

## What was built
- (Code-wise) Backend đã sử dụng Flyway (`V1__init_schema.sql`) kết hợp với Hibernate (`ddl-auto: validate`).
- Local PostgreSQL schema đã được cập nhật (`max_value`, `min_value` for devices).

## Verification
- Hibernate startup passed trên local PostgreSQL mà không có lỗi thiếu column.
- Database production trên Neon.tech sẽ tự động nhận được toàn bộ cấu trúc bảng thông qua cơ chế tự động chạy Flyway khi Spring Boot khởi động lần đầu tiên.

## Next Steps
- Lấy `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` từ Neon.tech để đưa vào biến môi trường của backend.
