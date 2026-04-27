# CONCERNS.md — Technical Debt & Areas of Concern

**Last mapped:** 2026-04-27

---

## 🔴 High Priority Concerns

### 1. AI Analysis Service is a Stub
**File:** `backend/api/src/main/java/com/yoloFarm/api/service/AiAnalysisService.java`

The AI image analysis feature returns a **hardcoded mock response** with `confidence: 0.80` and label `"Mock result for {type}"`. There is no real ML model integration. Additionally:
- Analysis logs are stored **in-memory only** (`CopyOnWriteArrayList`) — lost on restart
- No persistent storage for uploaded images or results
- No integration with any real AI provider (Google Vision, OpenAI Vision, custom model)

**Impact:** Core feature is non-functional in production.

---

### 2. No Frontend Test Suite
The `frontend/` directory has no testing framework configured at all — no Vitest, Jest, Playwright, or Cypress. No `test` npm script exists.

**Impact:** Zero test coverage for all UI interactions, routing, state management, and API contracts from the frontend side. Regressions will only be caught by manual testing.

---

### 3. No Simulator Test Suite
`simulator/digital-twin/` has no Python tests. No `pytest` or `unittest` setup.

**Impact:** Simulation logic changes (profile loading, MQTT publishing, LISTEN/NOTIFY handling) have no automated regression safety net.

---

### 4. `ddl-auto: update` in Production
**File:** `backend/api/src/main/resources/application.yml`

```yaml
spring.jpa.hibernate.ddl-auto: update
```

Using `update` in production is dangerous: it can silently alter table schemas, fail to apply necessary migrations, and cannot roll back. Industry best practice is to use `validate` or `none` with a dedicated migration tool (Flyway/Liquibase).

**Impact:** Risk of schema corruption or missed migrations in production.

---

### 5. In-Memory Automation Runtime State
**File:** `backend/api/src/main/java/com/yoloFarm/api/service/automation/AutomationRuntimeStateService.java`

Automation state (cooldown timestamps, auto-ON timestamps) is stored in-memory. On backend restart:
- All cooldowns reset → rules may fire multiple times immediately after restart
- Auto-ON since timestamps lost → safety watchdog re-initializes timers from scratch (`markAutoCommand(..., "ON", now)`)

This is mitigated by the watchdog's graceful restart handling, but edge cases exist.

**Impact:** Brief period after restart where automation rules could behave incorrectly.

---

## 🟡 Medium Priority Concerns

### 6. MQTT Observer Not Thread-Safe for `attach/detach`
**File:** `backend/api/src/main/java/com/yoloFarm/api/service/mqtt/MqttReceiverService.java`

The `observers` list is an `ArrayList` iterated by `notifyObservers()` on the observer executor threads, while `attach()`/`detach()` are called from the Spring context thread:
```java
private final List<Observer> observers = new ArrayList<>();
```
No synchronization on `attach`/`detach` while executor threads could be iterating. In practice safe (observers set at `@PostConstruct` before any MQTT messages), but fragile for dynamic observer registration.

---

### 7. Feed Key Cache Inconsistency on Device Updates
**File:** `MqttReceiverService.java`

The `feedKeyCache` is warmed on device approve and evicted on device remove, but:
- If a device's `adafruitFeedKey` is changed via rename, the old cache entry is not automatically evicted
- `DeviceService.renameSyncTest` tests rename sync, but it's unclear if the MQTT cache is evicted on rename

**Impact:** Stale cache could route MQTT messages to wrong device after a feed key rename.

---

### 8. Hibernate `ddl-auto: update` — `devices` Table with Both H2 and PostgreSQL
The test scope uses H2, production uses PostgreSQL. Enum columns (`@Enumerated(EnumType.STRING)`) may behave differently between H2 (varchar) and PostgreSQL (varchar OK, but `CHECK` constraints differ). This mismatch could hide schema issues in tests.

---

### 9. No Rate Limiting on REST API
No rate limiting middleware is configured. Endpoints like `/api/auth/login` are vulnerable to brute-force attacks without throttling.

---

### 10. Adafruit IO Feed Quota Risk
**File:** `AdafruitApiService.java` docs note:
> "vd: quá hạn ngạch 10 feeds" (e.g. exceeding 10-feed quota)

Adafruit IO free tier limits feeds to ~10. If users register many devices, feed creation will fail with 403/422. No graceful quota management or upgrade path is implemented.

---

### 11. WebSocket Auth — CONNECT Only
**File:** `WebSocketAuthChannelInterceptor.java`

JWT is validated on STOMP CONNECT. If a JWT expires during an active WebSocket session, the session remains open and continues receiving messages. No session invalidation on token expiry.

---

### 12. Missing CORS Configuration Detail
The `WS_ALLOWED_ORIGINS` env var configures WebSocket CORS, but it's unclear if the REST API CORS configuration is separate and equally locked down. Mixed-origin scenarios (frontend on different port/domain) may have gaps.

---

## 🟢 Low Priority / Future Improvements

### 13. PlantUML Diagrams May Be Outdated
`backend/plantUML/architecture_system.puml` is a 72-line diagram. As the codebase evolves, diagrams may fall out of sync with actual code. No automated diagram regeneration is in place.

### 14. Simulator `profiles.json` Not Version-Controlled Clearly
`profiles.json` (the actual simulation config) and `profiles.example.json` both exist. If the `.gitignore` excludes `profiles.json`, simulation configs are lost between environments. Currently `.gitignore` does not appear to exclude it, but this should be verified.

### 15. Vietnamese / Mixed-Language Codebase
Comments and log messages are in Vietnamese (e.g., `"Đã Subscribe thành công"`, `"Nhận dữ liệu"`). This limits accessibility for non-Vietnamese contributors but is a deliberate team choice.

### 16. No OpenAPI Code Generation Pipeline
`openAPI.yaml` exists (~52KB) but is not wired to any code generation tool. Frontend API types must be maintained manually, risking type drift.

### 17. AI Analysis Logs — Not Persistent
AI analysis logs stored in `CopyOnWriteArrayList` are in-memory only. No database table for `ai_logs` exists. This is explicitly deferred tech debt.

### 18. `spring-boot-starter-data-jpa-test` and Newer Starters
The test dependencies reference `spring-boot-starter-data-jpa-test`, `spring-boot-starter-webmvc-test`, and `spring-boot-starter-validation-test` — these are non-standard starter names and may not resolve correctly in Maven Central for Spring Boot 4.x. The standard approach is `spring-boot-starter-test` only. This could cause build failures.

---

## Summary

| Severity | Count | Top Issues |
|---|---|---|
| 🔴 High | 5 | AI stub, no frontend tests, no simulator tests, `ddl-auto: update`, in-memory state |
| 🟡 Medium | 7 | Thread safety, feed cache staleness, no rate limiting, quota risk, WS auth gaps |
| 🟢 Low | 6 | Diagram drift, language barrier, no code gen, AI log persistence |
