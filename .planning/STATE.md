# STATE.md — YoloFarm Project State

**Last updated:** 2026-04-27
**Current milestone:** v1.0 — Production-ready & Stable

## Project Reference

See: `.planning/PROJECT.md` (updated 2026-04-27)

**Core value:** Nông dân có thể tin tưởng rằng dữ liệu cảm biến chính xác và máy bơm bật/tắt đúng theo quy tắc.
**Current focus:** Phase 3 — Frontend Test Suite

## Milestone Progress

| Phase | Name | Status |
|---|---|---|
| 1 | Per-Device Threshold Configuration | ✅ Complete |
| 2 | Stability & Security Hardening | ✅ Complete |
| 3 | Frontend Test Suite | 🔵 Next up |
| 4 | Production Deployment | ⬜ Pending |

## Completed Work (Pre-GSD)

Các fix đã thực hiện trước khi khởi tạo GSD (không có Phase artifacts):

- ✅ Dịch toàn bộ log tiếng Việt sang tiếng Anh (14 files)
- ✅ Hạ log level MQTT noise từ INFO → DEBUG
- ✅ Fix Digital Twin Simulator: gửi `ON`/`OFF` thay vì `1.00`/`0.00` cho actuators
- ✅ Fix Simulator: đọc `is_active` từ DB khi khởi động (tránh mất state sau restart)

## Open Questions

- [ ] HuggingFace Spaces có hỗ trợ WebSocket (STOMP/SockJS) đầy đủ không? → Cần verify khi Phase 4
- [ ] Neon.tech connection pooling có tương thích với HikariCP của Spring Boot không? → Cần verify khi Phase 4

## Notes

- Digital Twin Simulator chạy **local only** — không cần deploy lên server
- `no_auto_commit: true` — mọi thay đổi code cần user review trước khi commit
