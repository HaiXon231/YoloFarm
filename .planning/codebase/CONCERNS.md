# CONCERNS.md — Technical Debt & Areas of Concern (Backend)

**Last mapped:** 2026-04-28

---

## 🔴 High Priority Concerns

### 1. AI Analysis Service is a Stub
**File:** `backend/api/src/main/java/com/yoloFarm/api/service/AiAnalysisService.java`

The AI image analysis feature returns a **hardcoded mock response** with `confidence: 0.80` and label `"Mock result for {type}"`. Additionally:
- Analysis logs are **in-memory only** (`CopyOnWriteArrayList`) — lost on restart
- No persistent storage for uploaded images or results
- No real AI provider integration

**Impact:** Core feature is non-functional in production.

---

### 2. `ddl-auto: update` While Flyway Migrations Exist
**File:** `backend/api/src/main/resources/application.yml`

```yaml
spring.jpa.hibernate.ddl-auto: update
```

Flyway migrations exist in `db/migration`, but Hibernate is still set to `update`. This can cause silent schema drift and conflicts with migration history. Best practice is to use `validate` or `none` in production when Flyway is present.

**Impact:** Risk of schema corruption or missed migrations.

---

## 🟡 Medium Priority Concerns

### 3. In-Memory Automation Runtime State
**File:** `backend/api/src/main/java/com/yoloFarm/api/service/automation/AutomationRuntimeStateService.java`

Cooldowns and auto-ON state are stored in-memory. On backend restart:
- Cooldowns reset → rules may fire immediately
- Auto-ON timestamps lost → watchdog timers reset

**Impact:** Automation behavior can be inconsistent after restarts.

---

### 4. Login Rate Limiting is In-Memory Only
**File:** `backend/api/src/main/java/com/yoloFarm/api/security/RateLimitFilter.java`

Bucket4j buckets are in-memory and keyed by `request.getRemoteAddr()`. In a multi-instance or proxy setup:
- Limits are not shared across nodes
- IP addresses may be incorrect without proxy headers

**Impact:** Rate limiting can be bypassed or misapplied in production deployments.

---

### 5. WebSocket Auth Validates Only on CONNECT
**File:** `backend/api/src/main/java/com/yoloFarm/api/security/WebSocketAuthChannelInterceptor.java`

JWT validation occurs on STOMP CONNECT; token expiry during active sessions is not re-validated.

**Impact:** Clients may continue receiving messages after token expiration.

---

### 6. MQTT Observer Backpressure Can Block Callback Thread
**File:** `backend/api/src/main/java/com/yoloFarm/api/service/mqtt/MqttReceiverService.java`

When the observer executor queue is full, `CallerRunsPolicy` runs observers on the MQTT callback thread.

**Impact:** Under load, MQTT processing can block and delay telemetry ingestion.

---

## 🟢 Low Priority / Future Improvements

### 7. REST CORS Allowlist is Hard-Coded
**File:** `backend/api/src/main/java/com/yoloFarm/api/config/CorsConfig.java`

Allowed origins are hard-coded for localhost/Vercel. WebSocket origins are configured via env var separately.

**Impact:** Environment-specific changes require code edits; REST vs WS CORS can drift.

---

### 8. OpenAPI Spec Not Wired to Code Generation
`backend/api/openAPI.yaml` exists but is not used for client or server code generation.

**Impact:** Manual API type drift risk.
