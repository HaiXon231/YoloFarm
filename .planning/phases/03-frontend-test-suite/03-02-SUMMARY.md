# Plan 03-02: Page Integration Tests

## What was built
- Tạo bộ integration test `FarmDetailPage.test.tsx` cho trang `FarmDetailPage`.
- Cấu hình mock cho thư viện `axios` (API requests), `recharts` (tránh lỗi jsdom) và `websocket`.
- Bọc component trong `MemoryRouter` để cung cấp params `farmId`.
- Viết 4 kịch bản kiểm thử:
  1. Hiển thị loading state ban đầu.
  2. Hiển thị thông tin farm và thiết bị sau khi fetch API thành công.
  3. Cập nhật giao diện khi chuyển sang tab 'Biểu đồ thống kê' (TelemetryTab).
  4. Gửi đúng request điều khiển khi click nút chuyển đổi Actuator (từ 'Tắt' sang 'Bật' với command `ON` đến `/devices/:id/command`).

## Verification
- Tất cả các page tests đã pass thành công.
- Không có lỗi Unhandled Promise Rejection hoặc JS DOM errors.

## Next Steps
- Phase 3 đã hoàn tất. Tiến hành Phase 4 để chuẩn bị deploy ứng dụng lên môi trường Production.
