# Requirements: YoloFarm

**Defined:** 2026-04-27
**Core Value:** Nông dân có thể tin tưởng rằng dữ liệu cảm biến chính xác và máy bơm bật/tắt đúng theo quy tắc — không bị gián đoạn bởi bug runtime hay schema migration.

## v1 Requirements

Requirements cho Milestone v1.0: Production-ready + Stable.

### Per-Device Configuration

- [ ] **CFG-01**: Farmer có thể chỉnh ngưỡng giá trị hợp lệ (min/max) cho từng thiết bị cảm biến trong UI
- [ ] **CFG-02**: Backend lưu và validate payload sensor data theo ngưỡng của từng thiết bị
- [ ] **CFG-03**: Digital Twin Simulator đọc ngưỡng min/max từ DB (thay vì hardcode trong profiles.json) để tạo giá trị mô phỏng thực tế hơn

### Schema & Database Safety

- [ ] **DB-01**: Thay `ddl-auto: update` bằng `ddl-auto: validate` + Flyway migration
- [ ] **DB-02**: Tạo Flyway baseline migration script từ schema hiện tại
- [ ] **DB-03**: Neon.tech PostgreSQL được cấu hình và kết nối thành công từ production backend

### Testing

- [ ] **TEST-01**: Backend integration test cho Rule Engine pipeline (sensor data → rule trigger → actuator command)
- [ ] **TEST-02**: Backend integration test cho AutoIrrigationSafetyService (watchdog tự tắt sau `maxAutoOnMinutes`)
- [ ] **TEST-03**: Frontend unit test cho các store Zustand (`authStore`, `notificationStore`)
- [ ] **TEST-04**: Frontend integration test cho FarmDetailPage (hiển thị telemetry + điều khiển thiết bị)

### Security & Stability

- [ ] **SEC-01**: Rate limiting trên `/api/auth/login` (tối đa N request/phút/IP)
- [ ] **SEC-02**: Feed key cache eviction khi farmer rename thiết bị (tránh route nhầm MQTT)
- [ ] **SEC-03**: Adafruit quota guard: Trả về lỗi có ý nghĩa khi vượt quá 10 feeds thay vì silent fail
- [ ] **SEC-04**: MQTT observer list dùng `CopyOnWriteArrayList` thay vì `ArrayList` để thread-safe

### Deployment

- [ ] **DEPLOY-01**: Dockerfile multi-stage build cho Spring Boot backend (Java 25)
- [ ] **DEPLOY-02**: Backend deploy thành công lên HuggingFace Spaces (port 7860)
- [ ] **DEPLOY-03**: Frontend deploy thành công lên Vercel với env vars đúng (API URL, WS URL)
- [ ] **DEPLOY-04**: Environment variables production được cấu hình đầy đủ (Neon DB, Adafruit, JWT secret)

## v2 Requirements

Deferred — thực hiện sau khi v1.0 ổn định.

### AI Integration

- **AI-01**: Tích hợp AI provider thực (Google Vision / OpenAI Vision) cho AiAnalysisService
- **AI-02**: Lưu kết quả phân tích AI vào bảng `ai_logs` trong PostgreSQL
- **AI-03**: Frontend hiển thị lịch sử phân tích AI cho farmer

### Advanced Automation

- **AUTO-01**: WebSocket session invalidation khi JWT hết hạn trong active session
- **AUTO-02**: AutomationRuntimeStateService persist cooldown timestamps vào DB (tránh mất state khi restart)

## Out of Scope

| Feature | Reason |
|---|---|
| Mobile app | Web-first là đủ cho v1.0 |
| OpenAPI code generation | Tech debt thấp, không ảnh hưởng runtime |
| Multi-tenant quota pooling | Ngoài scope Adafruit free tier |
| PlantUML auto-sync | Low priority |
| Real-time chat / notifications ngoài farm | Không thuộc core value |

## Traceability

| Requirement | Phase | Status |
|---|---|---|
| CFG-01 | Phase 1 | Pending |
| CFG-02 | Phase 1 | Pending |
| CFG-03 | Phase 1 | Pending |
| DB-01 | Phase 2 | Pending |
| DB-02 | Phase 2 | Pending |
| DB-03 | Phase 4 | Pending |
| TEST-01 | Phase 2 | Pending |
| TEST-02 | Phase 2 | Pending |
| TEST-03 | Phase 3 | Pending |
| TEST-04 | Phase 3 | Pending |
| SEC-01 | Phase 2 | Pending |
| SEC-02 | Phase 2 | Pending |
| SEC-03 | Phase 2 | Pending |
| SEC-04 | Phase 2 | Pending |
| DEPLOY-01 | Phase 4 | Pending |
| DEPLOY-02 | Phase 4 | Pending |
| DEPLOY-03 | Phase 4 | Pending |
| DEPLOY-04 | Phase 4 | Pending |

**Coverage:**
- v1 requirements: 18 total
- Mapped to phases: 18
- Unmapped: 0 ✓

---
*Requirements defined: 2026-04-27*
*Last updated: 2026-04-27 after initial definition*
