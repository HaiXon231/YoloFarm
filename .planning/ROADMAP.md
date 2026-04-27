# Roadmap: YoloFarm

## Overview

YoloFarm v1.0 tập trung vào việc ổn định hóa hệ thống đang chạy: thêm cấu hình ngưỡng per-device cho nông dân, tăng độ bảo mật và kiểm thử backend, bổ sung test suite cho frontend, và cuối cùng deploy toàn bộ hệ thống lên production (Vercel + HuggingFace + Neon.tech).

## Phases

- [x] **Phase 1: Per-Device Threshold Configuration** - Farmer chỉnh ngưỡng min/max cho từng cảm biến; Simulator dùng ngưỡng từ DB
- [x] **Phase 2: Stability & Security Hardening** - Flyway migration, rate limiting, MQTT thread-safety, backend tests
- [x] **Phase 3: Frontend Test Suite** - Vitest + RTL cho Zustand stores và FarmDetailPage
- [ ] **Phase 4: Production Deployment** - Dockerfile, HuggingFace Spaces, Vercel, Neon.tech

## Phase Details

### Phase 1: Per-Device Threshold Configuration
**Goal**: Farmer có thể cấu hình ngưỡng giá trị hợp lệ (min/max) cho từng thiết bị cảm biến trong UI. Digital Twin Simulator đọc ngưỡng này từ DB để tạo dữ liệu mô phỏng thực tế hơn.
**Depends on**: Nothing (first phase)
**Requirements**: CFG-01, CFG-02, CFG-03
**Success Criteria** (what must be TRUE):
  1. Farmer có thể mở trang Farm Detail và chỉnh min/max cho từng cảm biến
  2. Backend lưu ngưỡng và validate payload sensor data, log warning nếu vượt ngưỡng
  3. Simulator khởi động và dùng min/max từ DB (không cần chỉnh profiles.json thủ công)
**Plans**: TBD

Plans:
- [x] 01-01: Schema & API — Thêm cột min/max vào DB, tạo endpoint update threshold
- [x] 01-02: Frontend UI — Thêm form chỉnh ngưỡng trong FarmDetailPage
- [x] 01-03: Simulator — Đọc ngưỡng min/max từ DB khi khởi động

### Phase 2: Stability & Security Hardening
**Goal**: Fix các lỗi bảo mật và kỹ thuật tồn đọng. Thêm test suite cho backend. Schema migration an toàn với Flyway.
**Depends on**: Phase 1
**Requirements**: DB-01, DB-02, TEST-01, TEST-02, SEC-01, SEC-02, SEC-03, SEC-04
**Success Criteria** (what must be TRUE):
  1. `ddl-auto: validate` — ứng dụng khởi động thành công với Flyway migration V1
  2. `/api/auth/login` trả về 429 khi vượt rate limit
  3. Rule Engine integration test pass (sensor data → rule trigger → actuator command)
  4. Safety watchdog integration test pass (auto-off sau maxAutoOnMinutes)
  5. Rename thiết bị không gây route nhầm MQTT (cache evicted)
  6. Vượt quota Adafruit trả về lỗi có thông báo rõ ràng
**Plans**: TBD

Plans:
- [x] 02-01: Flyway Migration — Baseline V1 từ schema hiện tại, đổi ddl-auto
- [x] 02-02: Security Fixes — Rate limiting, MQTT thread-safety, cache eviction, Adafruit quota guard
- [x] 02-03: Backend Tests — Integration tests cho Rule Engine và Safety Watchdog

### Phase 3: Frontend Test Suite
**Goal**: Thiết lập Vitest + React Testing Library và viết tests cho các luồng UI quan trọng nhất.
**Depends on**: Phase 2
**Requirements**: TEST-03, TEST-04
**Success Criteria** (what must be TRUE):
  1. `npm test` chạy thành công, không có test nào fail
  2. authStore và notificationStore có unit test coverage
  3. FarmDetailPage có integration test (render chart, điều khiển thiết bị)
**Plans**: TBD

Plans:
- [x] 03-01: Test Setup & Store Tests — Cấu hình Vitest, viết test cho authStore và notificationStore
- [x] 03-02: Page Integration Tests — FarmDetailPage tests

### Phase 4: Production Deployment
**Goal**: Deploy đầy đủ lên production. FE trên Vercel, BE trên HuggingFace Spaces (Docker), DB trên Neon.tech.
**Depends on**: Phase 3
**Requirements**: DB-03, DEPLOY-01, DEPLOY-02, DEPLOY-03, DEPLOY-04
**Success Criteria** (what must be TRUE):
  1. Dockerfile build thành công và backend chạy trên port 7860
  2. Backend accessible tại HuggingFace Spaces URL
  3. Frontend deploy thành công trên Vercel, connect được với backend
  4. E2E smoke test: đăng nhập → xem farm → xem telemetry → bật/tắt thiết bị
**Plans**: TBD

Plans:
- [ ] 04-01: Database — Tạo Neon.tech project, chạy Flyway migration V1
- [ ] 04-02: Backend Docker — Multi-stage Dockerfile, HuggingFace Spaces deploy
- [ ] 04-03: Frontend Deploy — Vercel deploy với env vars production

## Progress

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Per-Device Threshold Config | 3/3 | Complete | 2026-04-27 |
| 2. Stability & Security Hardening | 3/3 | Complete | 2026-04-27 |
| 3. Frontend Test Suite | 2/2 | Complete | 2026-04-27 |
| 4. Production Deployment | 0/3 | Not started | - |
