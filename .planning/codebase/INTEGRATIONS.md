# INTEGRATIONS.md — External Services & APIs

**Last mapped:** 2026-04-27

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
  - **Publish:** Backend publishes actuator ON/OFF commands to specific feed topics
- **Client bean:** `IMqttClient` (Spring-managed singleton)
- **Receiver service:** `backend/api/src/main/java/com/yoloFarm/api/service/mqtt/MqttReceiverService.java`
- **Sender service:** `backend/api/src/main/java/com/yoloFarm/api/service/impl/MqttSenderServiceImpl.java`

### REST API Integration (Backend → Adafruit)
- **Purpose:** CRUD feed management (create/rename/delete feeds when devices are registered)
- **API version:** Adafruit IO REST v2
- **Service interface:** `backend/api/src/main/java/com/yoloFarm/api/service/AdafruitApiService.java`
- **Implementation:** `backend/api/src/main/java/com/yoloFarm/api/service/impl/AdafruitApiServiceImpl.java`
- **Operations:** `createFeed()`, `updateFeedName()`, `deleteFeed()`

### MQTT Integration (Simulator ↔ Adafruit)
- **Library:** paho-mqtt 1.6.1
- **TLS:** `ssl.CERT_REQUIRED`
- **Publish:** Simulator publishes synthetic sensor values per device feed
- **Subscribe:** Simulator subscribes to actuator feed topics to receive ON/OFF commands
- **File:** `simulator/digital-twin/main.py` — `DigitalTwinManager`

---

## 2. PostgreSQL Database

**Purpose:** Persistent storage for all domain entities.

### Backend Connection
- **Driver:** `org.postgresql.Driver`
- **JDBC URL:** `jdbc:postgresql://localhost:5432/yolofarm_db`
- **ORM:** Hibernate JPA (`ddl-auto: update`)
- **Direct JDBC:** `JdbcTemplate` used in `MqttReceiverService` for atomic device-status updates (avoid Hibernate detached-entity issues)
- **Config:** `backend/api/src/main/resources/application.yml`
- **Credentials:** `${DB_USERNAME}` / `${DB_PASSWORD}` from environment

### Simulator Connection (psycopg2)
- **Library:** psycopg2-binary 2.9.10
- **Purpose:** Poll active devices, sync device registry
- **LISTEN/NOTIFY:** Simulator uses `LISTEN device_events` on a persistent connection; backend fires `NOTIFY device_events` when devices are approved/removed, triggering immediate simulator re-sync
- **Polling fallback:** `select.select()` with 5-second timeout if no NOTIFY received
- **File:** `simulator/digital-twin/main.py` — `_db_conn()`, `run()` method

---

## 3. WebSocket / STOMP (Backend → Frontend)

**Purpose:** Real-time push of telemetry, device status changes, and notifications.

### Backend
- **Library:** `spring-boot-starter-websocket` + STOMP message protocol
- **SimpMessagingTemplate:** Used across services to push to topic destinations
- **CORS:** Configured via `${WS_ALLOWED_ORIGINS}` (default `http://localhost:3000`)
- **Auth:** `WebSocketAuthChannelInterceptor` validates JWT on STOMP CONNECT

### Topic Map
| Topic | Publisher | Payload |
|---|---|---|
| `/topic/farm/{farmId}/telemetry` | `WebSocketNotifierObserver` | `SensorData` |
| `/topic/farm/{farmId}/device-status` | `MqttReceiverService`, `DeviceHeartbeatService` | `[{deviceId, connectionStatus}]` |
| `/topic/farm/{farmId}/notifications` | `NotificationService` | `NotificationResponse` |
| `/topic/admin/stats-changed` | `MqttReceiverService`, `DeviceHeartbeatService` | `{reason}` |

### Frontend
- **Libraries:** `@stomp/stompjs` `^7.0.0` + `sockjs-client` `^1.6.1`
- **Store:** `frontend/src/stores/notificationStore.ts`
- **Connection:** SockJS transport fallback for WebSocket

---

## 4. JWT / Spring Security

**Purpose:** Stateless authentication for REST and WebSocket connections.

- **Library:** JJWT `0.11.5` (api + impl + jackson)
- **Algorithm:** HS256 (HMAC-SHA256) with Base64-encoded secret
- **Token lifetime:** 86400000 ms (24 hours)
- **Filter:** `JwtAuthenticationFilter` — intercepts HTTP requests, validates Bearer token
- **WebSocket Auth:** `WebSocketAuthChannelInterceptor` — validates token on STOMP CONNECT frame
- **Services:** `backend/api/src/main/java/com/yoloFarm/api/service/security/JwtService.java`
- **Error Handlers:**
  - `RestAuthenticationEntryPoint` — 401 on missing/invalid auth
  - `RestAccessDeniedHandler` — 403 on insufficient permissions

---

## 5. AI Analysis (Stub / Future)

**Purpose:** Image-based plant disease detection (currently a mock implementation).

- **Service:** `backend/api/src/main/java/com/yoloFarm/api/service/AiAnalysisService.java`
- **Current state:** Returns a hardcoded mock result (`confidence: 0.80`, label: `"Mock result for {type}"`)
- **Logs:** In-memory `CopyOnWriteArrayList` (lost on restart — no persistence yet)
- **Endpoint:** Accepts `MultipartFile` image upload; validates farm ownership before processing
- **Note:** No real AI/ML provider integrated; placeholder for future model integration (e.g. Google Vision, custom TensorFlow Serving)

---

## 6. Spring Boot Actuator

**Purpose:** Health checks and operational monitoring.

- **Dependency:** `spring-boot-starter-actuator`
- **Endpoints:** `/actuator/health` (default exposure)
- **No custom metrics** configured yet

---

## Summary Table

| Integration | Direction | Protocol | Status |
|---|---|---|---|
| Adafruit IO MQTT | Bidirectional | MQTT TLS | ✅ Active |
| Adafruit IO REST | Backend → Adafruit | HTTPS REST v2 | ✅ Active |
| PostgreSQL | Backend ↔ DB | JDBC / JPA | ✅ Active |
| PostgreSQL LISTEN/NOTIFY | DB → Simulator | Native psycopg2 | ✅ Active |
| WebSocket STOMP | Backend → Frontend | WS/SockJS | ✅ Active |
| JWT Auth | Frontend → Backend | HTTP Bearer | ✅ Active |
| AI Analysis | Frontend → Backend | HTTP REST | ⚠️ Mock only |
