# ARCHITECTURE.md — Backend Architecture

**Last mapped:** 2026-04-28

---

## Scope
- `backend/api/` only

---

## System Overview

```
┌──────────────────────┐        MQTT (TLS)         ┌──────────────────────┐
│   Adafruit IO Cloud  │ ◄──────────────────────── │  Spring Boot Backend  │
│   (MQTT Broker)      │ ─────────────────────────►│  (Java 21, Port 8080) │
└──────────────────────┘                            └──────────┬───────────┘
                                                               │ REST + STOMP/WS
                                                               ▼
                                                     ┌────────────────────┐
                                                     │   Frontend Client   │
                                                     └────────────────────┘
                                                               ▲
                                                               │ JDBC / JPA
                                                               ▼
                                                     ┌────────────────────┐
                                                     │   PostgreSQL DB     │
                                                     └────────────────────┘
```

---

## Architectural Pattern

### Layered + Event-Driven

```
Controller Layer  →  Service Layer  →  Repository Layer  →  Database
                         ↕
                  MQTT Event Bus
                  (Observer Pattern)
                         ↕
                  WebSocket Push
```

**Design patterns applied:**

| Pattern | Where Used |
|---|---|
| **Observer** | `MqttReceiverService` (Subject) notifies `DatabaseLoggerObserver`, `RuleEngineObserver`, `WebSocketNotifierObserver` |
| **Strategy** | `IrrigationContext` selects between `AutoThresholdStrategy`, `ManualStrategy`, `ScheduledStrategy` |
| **Repository** | Spring Data JPA repositories per entity |
| **Chain of Responsibility** | Spring Security filter chain (`JwtAuthenticationFilter` → controllers) |
| **Decorator** | Lombok `@Slf4j` injected logging |

---

## Data Flow

### Sensor Reading Flow (Adafruit MQTT → Backend → WebSocket)
```
1. Adafruit publishes telemetry to {username}/feeds/{feed_key}
2. MqttReceiverService.messageArrived() fires
   a. Resolve device by feed alias (cache → DB fallback)
   b. Update device ONLINE + last_seen via JdbcTemplate
   c. Wrap into SensorData record
   d. notifyObservers(sensorData) on bounded executor
3. DatabaseLoggerObserver persists TelemetryData
4. RuleEngineObserver evaluates rules → may send actuator command
5. WebSocketNotifierObserver pushes /topic/farm/{farmId}/telemetry
```

### Actuator Control Flow (REST → MQTT)
```
1. REST command → ControlService
2. IrrigationContext.executeControl(...)
3. MqttSenderService publishes command to Adafruit feed topic
```

### Device Approval Flow (Admin → Adafruit + DB)
```
1. DeviceService.approveDevice()
2. AdafruitApiService.createFeed()
3. Device saved with feed key
4. NOTIFY device_events to trigger downstream sync (simulator listener)
```

### WebSocket Auth Flow
```
CONNECT → WebSocketAuthChannelInterceptor (JWT validation)
SUBSCRIBE /topic/farm/{farmId}/telemetry → ownership check (farm owner or admin)
```

---

## Backend Layer Details

### Entry Points
- `backend/api/src/main/java/com/yoloFarm/api/ApiApplication.java`
- WebSocket endpoint `/ws` (SockJS)

### Configuration (`config/`)
- `SecurityConfig` — JWT filter chain, stateless sessions
- `CorsConfig` — REST CORS allowlist
- `WebSocketConfig` — STOMP broker + inbound auth
- `MqttConfig` — MQTT client lifecycle + subscribe

### Controller Layer (`controller/`)
- `AuthController` — login/register
- `FarmController` — farm CRUD
- `DeviceController` — device lifecycle + control
- `RuleController` — rule CRUD
- `UserController` — profile management
- `AdminController` — admin workflows

### Service Layer (`service/`)
- `DeviceService` — device lifecycle + Adafruit feed sync
- `RuleService` / `RuleSchedulerService` — rule validation and scheduling
- `TelemetryService` — historical telemetry queries
- `NotificationService` — system notifications + WebSocket push
- `AiAnalysisService` — image analysis stub
- `DeviceHeartbeatService` — offline detection scheduler
- `AutoIrrigationSafetyService` — safety watchdog
- `ControlService` — manual actuator control orchestration

### MQTT Subsystem (`service/mqtt/`)
- `MqttReceiverService` — Subject; wildcard subscribe; notifyObservers
- `MqttSenderServiceImpl` — publish commands
- Observers: `DatabaseLoggerObserver`, `RuleEngineObserver`, `WebSocketNotifierObserver`

### Strategy Subsystem (`service/strategy/`)
- `IrrigationStrategy` (interface)
- `IrrigationContext`, `ManualStrategy`, `AutoThresholdStrategy`, `ScheduledStrategy`

### Security Layer (`security/`)
- `JwtAuthenticationFilter` — Bearer token validation
- `WebSocketAuthChannelInterceptor` — JWT validation for STOMP
- `RestAuthenticationEntryPoint`, `RestAccessDeniedHandler`
- `RateLimitFilter` — login throttling (Bucket4j)

### Repository Layer (`repository/`)
- One `JpaRepository` per entity + `repository/projection/` for admin views

---
