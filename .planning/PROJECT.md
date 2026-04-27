# YoloFarm

## What This Is

YoloFarm là nền tảng quản lý trang trại IoT thông minh dành cho nông dân (Farmer) và quản trị viên (Admin). Hệ thống cho phép giám sát cảm biến theo thời gian thực, điều khiển thiết bị tự động hóa (máy bơm, đèn) qua quy tắc ngưỡng, và quản lý vòng đời thiết bị thông qua Adafruit IO làm MQTT broker.

## Core Value

Nông dân có thể nhìn thấy dữ liệu cảm biến theo thời gian thực và tin tưởng rằng máy bơm sẽ bật/tắt đúng lúc theo quy tắc đã cấu hình — không bị ngắt đột ngột, không mất trạng thái.

## Requirements

### Validated

<!-- Đã hoạt động ổn định trong codebase hiện tại -->

- ✓ Farmer đăng ký, đăng nhập bằng JWT — existing
- ✓ Admin duyệt thiết bị và cấp feed key Adafruit — existing
- ✓ Thiết bị gửi telemetry qua MQTT → Backend nhận → Lưu DB + push WebSocket — existing
- ✓ Frontend hiển thị biểu đồ telemetry realtime (Recharts) — existing
- ✓ Farmer tạo quy tắc tự động hóa (Rule) với ngưỡng cảm biến — existing
- ✓ Digital Twin Simulator mô phỏng dữ liệu cảm biến và đồng bộ trạng thái actuator từ DB — existing (vừa fix)
- ✓ Log backend toàn bộ bằng tiếng Anh — existing (vừa fix)

### Active

<!-- Mục tiêu Milestone v1.0: Production-ready + Feature-complete -->

- [ ] Per-device threshold configuration: Farmer có thể chỉnh ngưỡng nhận giá trị hợp lệ (min/max) cho từng loại thiết bị cảm biến
- [ ] Schema migration an toàn: Thay `ddl-auto: update` bằng Flyway để deploy production không bị lỗi schema
- [ ] Frontend test suite: Vitest + React Testing Library cho các flow quan trọng
- [ ] Backend integration tests: Kiểm tra rule engine, safety watchdog và MQTT observer pipeline
- [ ] Rate limiting trên REST API: Bảo vệ `/api/auth/login` và các endpoint nhạy cảm
- [ ] Feed key cache eviction khi rename thiết bị: Tránh route nhầm MQTT sau đổi tên
- [ ] Adafruit quota management: Thông báo rõ ràng khi vượt quota 10 feeds
- [ ] Deploy pipeline hoàn chỉnh: FE → Vercel, BE → HuggingFace Spaces (Docker), DB → Neon.tech

### Out of Scope

- AI image analysis — Tính năng bonus, thực hiện sau khi toàn bộ v1.0 ổn định
- Mobile app — Web-first là đủ cho MVP
- Multi-zone / multi-farm phức tạp — Kiến trúc hiện tại đã hỗ trợ nhiều farm, không cần thêm
- OpenAPI code generation pipeline — Tech debt thấp, không ảnh hưởng runtime
- Real-time PlantUML sync — Low priority

## Context

- **Codebase hiện tại:** Brownfield — Backend Spring Boot 4.0.4 (Java 25), Frontend React 19 + Vite, Python Digital Twin Simulator, PostgreSQL
- **MQTT Broker:** Adafruit IO (TLS, free tier giới hạn 10 feeds)
- **Các lỗi đã fix:** Simulator desync (gửi `1.00` thay vì `ON`), mất state khi restart, log tiếng Việt
- **Vấn đề tồn đọng chính:** `ddl-auto: update`, thiếu test suite, thiếu rate limiting, feed cache staleness

## Constraints

- **Tech Stack:** Không thay đổi stack hiện tại (Spring Boot / React / Python)
- **Budget:** Free tier hoàn toàn — Vercel (FE), HuggingFace Spaces Docker (BE), Neon.tech (DB)
- **Adafruit IO:** Free tier giới hạn 10 feeds/account — cần quota guard
- **Java 25:** HuggingFace deploy phải dùng Docker multi-stage build
- **AI:** Hoàn toàn out of scope cho v1.0

## Key Decisions

| Decision | Rationale | Outcome |
|---|---|---|
| Giữ Adafruit IO làm MQTT broker | Đã tích hợp sẵn, chi phí zero cho dev/demo | — Pending |
| Flyway thay vì Liquibase | Đơn giản hơn cho Spring Boot, cộng đồng lớn hơn | — Pending |
| HuggingFace Spaces Docker cho Backend | Free, hỗ trợ Docker, phù hợp với Java 25 | — Pending |
| Neon.tech cho PostgreSQL | Free tier vĩnh viễn (0.5GB), serverless, tương thích hoàn toàn | — Pending |
| Không commit planning docs vào git | Giữ `.planning/` local-only theo yêu cầu | ✓ Good |
| Per-device threshold config | Mỗi loại cảm biến có min/max khác nhau, farmer cần điều chỉnh | — Pending |

## Evolution

Tài liệu này cập nhật tại mỗi milestone.

**Sau mỗi Phase transition:**
1. Requirements invalidated? → Chuyển sang Out of Scope với lý do
2. Requirements validated? → Chuyển sang Validated với phase reference
3. Quyết định mới? → Thêm vào Key Decisions

**Sau mỗi Milestone:**
1. Review toàn bộ sections
2. Core Value check — còn đúng không?
3. Audit Out of Scope — lý do còn valid không?

---
*Last updated: 2026-04-27 after project initialization (brownfield)*
