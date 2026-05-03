# INTEGRATIONS.md — External Services & APIs (Backend)

**Last mapped:** 2026-04-28

---

## Scope
- `backend/api/` integrations only

---

## 1. Adafruit IO (IoT Cloud Platform)

**Purpose:** Primary IoT message broker for sensor telemetry and actuator commands.

### MQTT Integration (Backend ↔ Adafruit)
- **Protocol:** MQTT over TLS (`ssl://io.adafruit.com:8883`)
- **Client:** Eclipse Paho `org.eclipse.paho.client.mqttv3` v1.2.5
- **Auth:** Username/password (`ADAFRUIT_USERNAME` / `ADAFRUIT_IO_KEY`)
- **Topic pattern:** `{username}/feeds/{feed_key}` (wildcard subscribe: `{username}/feeds/+`)
- **Direction:**
  - **Subscribe:** Backend subscribes to all feeds wildcard — receives sensor readings
  - **Publish:** Backend publishes actuator commands to specific feed topics
- **Client bean:** `IMqttClient` (Spring-managed singleton)
- **MQTT config:** `backend/api/src/main/java/com/yoloFarm/api/config/MqttConfig.java`
- **Receiver service:** `backend/api/src/main/java/com/yoloFarm/api/service/mqtt/MqttReceiverService.java`
- **Sender service:** `backend/api/src/main/java/com/yoloFarm/api/service/impl/MqttSenderServiceImpl.java`

### REST API Integration (Backend → Adafruit)
- **Purpose:** Feed lifecycle management (create/rename/delete feeds on device approval/rename/removal)
- **API version:** Adafruit IO REST v2
- **Service interface:** `backend/api/src/main/java/com/yoloFarm/api/service/AdafruitApiService.java`
- **Implementation:** `backend/api/src/main/java/com/yoloFarm/api/service/impl/AdafruitApiServiceImpl.java`
- **Operations:** `createFeed()`, `updateFeedName()`, `deleteFeed()`

---

## 2. PostgreSQL Database

**Purpose:** Persistent storage for all domain entities.

### Backend Connection
- **Driver:** `org.postgresql.Driver`
- **JDBC URL:** `jdbc:postgresql://localhost:5432/yolofarm_db` (default)
- **ORM:** Hibernate JPA (`ddl-auto: update`)
- **Migrations:** Flyway (`backend/api/src/main/resources/db/migration/`)
- **Direct JDBC:** `JdbcTemplate` used in `MqttReceiverService` for atomic device-status updates
- **Device events:** `DeviceService` issues `NOTIFY device_events` when devices are approved/removed
- **Config:** `backend/api/src/main/resources/application.yml`
- **Credentials:** `${DB_USERNAME}` / `${DB_PASSWORD}` from environment

---

## 3. WebSocket / STOMP (Backend → Frontend)

**Purpose:** Real-time push of telemetry, device status changes, and notifications.

### Backend
- **Library:** `spring-boot-starter-websocket` + STOMP message protocol
- **Endpoint:** `/ws` with SockJS fallback
- **Broker prefixes:** `/topic`, `/queue`; app prefix `/app`; user prefix `/user`
- **Auth:** `WebSocketAuthChannelInterceptor` validates JWT on STOMP CONNECT and enforces farm telemetry access
- **Config:** `backend/api/src/main/java/com/yoloFarm/api/config/WebSocketConfig.java`

---

## 4. JWT / Spring Security

**Purpose:** Stateless authentication for REST and WebSocket connections.

- **Library:** JJWT `0.11.5` (api + impl + jackson)
- **Algorithm:** HS256 (HMAC-SHA256) with Base64-encoded secret
- **Token lifetime:** 86400000 ms (24 hours)
- **Filter:** `JwtAuthenticationFilter` — validates Bearer tokens
- **Handlers:**
  - `RestAuthenticationEntryPoint` — 401 on missing/invalid auth
  - `RestAccessDeniedHandler` — 403 on insufficient permissions
- **Config:** `backend/api/src/main/java/com/yoloFarm/api/config/SecurityConfig.java`

---

## 5. CORS

- **REST CORS:** `CorsConfig` allows local dev and Vercel origins
- **WebSocket CORS:** `app.websocket.allowed-origins` in `application.yml`

---

## 6. Rate Limiting (Login)

- **Filter:** `RateLimitFilter` (Bucket4j)
- **Scope:** `/api/v1/auth/login`
- **Policy:** 5 requests/minute per IP (in-memory buckets)
- **File:** `backend/api/src/main/java/com/yoloFarm/api/security/RateLimitFilter.java`

---

## Summary Table

| Integration | Direction | Protocol | Status |
|---|---|---|---|
| Adafruit IO MQTT | Bidirectional | MQTT TLS | ✅ Active |
| Adafruit IO REST | Backend → Adafruit | HTTPS REST v2 | ✅ Active |
| PostgreSQL | Backend ↔ DB | JDBC / JPA | ✅ Active |
| WebSocket STOMP | Backend → Frontend | WS/SockJS | ✅ Active |
| JWT Auth | Frontend → Backend | HTTP Bearer | ✅ Active |
| Login Rate Limiting | Frontend → Backend | HTTP | ✅ Active |
