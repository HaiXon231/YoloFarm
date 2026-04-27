# ARCHITECTURE.md — System Architecture

**Last mapped:** 2026-04-27

---

## System Overview

YoloFarm is a **smart IoT farm management platform** with three runtime components:

```
┌─────────────────┐        MQTT (TLS)         ┌──────────────────────┐
│  Digital Twin   │ ◄──────────────────────── │   Adafruit IO Cloud  │
│  Simulator      │ ──────────────────────────►│   (MQTT Broker)      │
│  (Python)       │    subscribe actuator cmds └──────────────────────┘
└────────┬────────┘                                      ▲
         │ psycopg2                                      │ MQTT TLS
         │ LISTEN/NOTIFY                                 │
         ▼                                               │
┌─────────────────┐  REST + WebSocket   ┌───────────────┴──────────┐
│   PostgreSQL    │ ◄──────────────────►│   Spring Boot Backend     │
│   Database      │                    │   (Java 25, Port 8080)    │
└─────────────────┘                    └────────────┬──────────────┘
                                                    │ REST + STOMP/WS
                                                    ▼
                                       ┌────────────────────────────┐
                                       │  React Frontend (Vite SPA) │
                                       │  (TypeScript, Port 5173)   │
                                       └────────────────────────────┘
```

---

## Architectural Pattern

### Backend: Layered + Event-Driven

```
Controller Layer  →  Service Layer  →  Repository Layer  →  Database
                          ↕
                   MQTT Event Bus
                   (Observer Pattern)
                          ↕
                   WebSocket Push
```

**Design Patterns applied:**

| Pattern | Where Used |
|---|---|
| **Observer** | `MqttReceiverService` (Subject) notifies `DatabaseLoggerObserver`, `RuleEngineObserver`, `WebSocketNotifierObserver` |
| **Strategy** | `IrrigationContext` selects between `AutoThresholdStrategy`, `ManualStrategy`, `ScheduledStrategy` at runtime |
| **Repository** | Spring Data JPA repositories per entity |
| **Chain of Responsibility** | Spring Security filter chain (`JwtAuthenticationFilter` → controllers) |
| **Proxy/Decorator** | Lombok `@Slf4j` injected logging |

---

## Data Flow

### Sensor Reading Flow (Simulator → Frontend)
```
1. Simulator computes synthetic sensor value
2. Publishes to Adafruit MQTT topic: {username}/feeds/{feed_key}
3. Backend MqttReceiverService.messageArrived() fires:
   a. Resolves device from feedKey (ConcurrentHashMap cache → DB fallback)
   b. Updates device.connectionStatus = ONLINE via JdbcTemplate (avoid Hibernate detach)
   c. Wraps value in SensorData record
   d. notifyObservers(sensorData) — dispatches to thread pool (4 core / 8 max)
4. DatabaseLoggerObserver saves TelemetryData to PostgreSQL
5. RuleEngineObserver evaluates active rules → may trigger actuator command via IrrigationContext
6. WebSocketNotifierObserver pushes to /topic/farm/{farmId}/telemetry
7. Frontend notificationStore receives WS event → updates Recharts chart in real-time
```

### Actuator Control Flow (Frontend → Device)
```
1. User clicks ON/OFF on FarmDetailPage
2. Frontend POST /api/farms/{farmId}/devices/{deviceId}/control
3. ControlService.sendCommand() → IrrigationContext.executeControl(ManualStrategy, ...)
4. MqttSenderService publishes command to Adafruit feed topic
5. Adafruit routes message to Simulator (which subscribes actuator feeds)
6. Simulator updates runtime.actuator_state → publishes ON/OFF as next telemetry point
7. Backend re-receives the echoed value via MQTT → confirms state change
```

### Automation Rule Engine Flow
```
Sensor reading → RuleEngineObserver.update(SensorData)
  → findActiveRulesWithAssociations(deviceId)
  → For each rule: evaluateCondition(value, operator, threshold)
    → Check cooldown (AutomationRuntimeStateService)
    → IrrigationContext.executeControl(AutoThresholdStrategy, ...)
    → MqttSenderService.publish(command)
    → NotificationService.createSystemNotification(...)
    → Push WebSocket to farmer
```

### Device Heartbeat / Offline Detection
```
@Scheduled(cron = "30 * * * * *")  ← every minute at :30s
  → findStaleOnlineDevices(threshold = now - 5 minutes)
  → markStaleDevicesAsOffline(threshold)
  → Push /topic/farm/{farmId}/device-status with OFFLINE payloads
```

---

## Backend Layer Details

### Controller Layer (`controller/`)
- `AuthController` — login, register
- `FarmController` — farm CRUD for farmers
- `DeviceController` — device management (register, approve, rename, command)
- `RuleController` — automation rule CRUD
- `UserController` — profile management
- `AdminController` (inferred) — admin dashboard, stats, approval workflows

### Service Layer (`service/`)
- `AuthService` / `AuthServiceImpl` — JWT-based registration and login
- `FarmService` — farm ownership scoped CRUD
- `DeviceService` — device lifecycle including Adafruit feed sync
- `RuleService` — rule validation, pairing (trigger sensor ↔ action actuator)
- `TelemetryService` — historical telemetry queries
- `NotificationService` — system notification creation + WebSocket push
- `AiAnalysisService` — image analysis stub (mock)
- `AdminService` — admin-scoped stats, approvals, user management
- `DeviceHeartbeatService` — offline janitor scheduler
- `RuleSchedulerService` — cron-based rule execution
- `AutoIrrigationSafetyService` — safety watchdog (auto-off after maxAutoOnMinutes)
- `ControlService` — manual actuator control orchestration
- `DeviceModelService` — device model CRUD (admin)

### MQTT Subsystem (`service/mqtt/`)
- `MqttReceiverService` — Subject; wildcard subscribe; notifyObservers
- `MqttSenderService` (interface) / `MqttSenderServiceImpl` — publish commands
- **Observers:**
  - `DatabaseLoggerObserver` — persist TelemetryData
  - `RuleEngineObserver` — trigger automation rules
  - `WebSocketNotifierObserver` — push telemetry via SimpMessagingTemplate

### Strategy Subsystem (`service/strategy/`)
- `IrrigationStrategy` (interface) — `executeControl(farmId, deviceId, command): boolean`
- `IrrigationContext` — strategy router
- `ManualStrategy` — direct command from user
- `AutoThresholdStrategy` — rule engine / watchdog commands
- `ScheduledStrategy` — cron-triggered commands

### Security Layer (`security/`)
- `JwtAuthenticationFilter` — Bearer token extraction and validation per request
- `WebSocketAuthChannelInterceptor` — validates JWT on STOMP CONNECT
- `RestAuthenticationEntryPoint` — 401 JSON response
- `RestAccessDeniedHandler` — 403 JSON response

### Repository Layer (`repository/`)
- One `JpaRepository` per entity: `User`, `Farm`, `Device`, `DeviceModel`, `Rule`, `TelemetryData`, `Notification`
- Custom projections in `repository/projection/` for admin list views
- Key custom queries: `findStaleOnlineDevices`, `findActiveRulesWithAssociations`, `findActiveScheduledRulesWithAssociations`, `findActiveAutoActuatorsWithFarmAndOwner`

---

## Frontend Architecture

### Routing (react-router-dom v7)
- `PublicRoute` — redirects authenticated users away from `/login`, `/register`
- `ProtectedRoute` — validates auth + optional `requiredRole` prop (FARMER / ADMIN)
- `MainLayout` — shared navigation shell for all protected pages

### State Management (Zustand)
- `authStore` — token, user profile, `isAuthenticated`, `loadFromStorage()`, `fetchProfile()`
- `notificationStore` — WebSocket connection lifecycle, notification list, unread count

### Page Hierarchy
```
/login → LoginPage
/register → RegisterPage
/farms → FarmsPage (FARMER)
/farms/:farmId → FarmDetailPage (FARMER) — main dashboard with telemetry + controls
/admin/dashboard → AdminDashboardPage (ADMIN)
/admin/device-requests → DeviceRequestsPage (ADMIN)
/admin/device-models → DeviceModelsPage (ADMIN)
/profile → ProfilePage (any authenticated)
```

### Component Domains (`src/components/`)
- `layout/` — `MainLayout`
- `guards/` — `PublicRoute`, `ProtectedRoute`
- `farms/` — farm list and card components
- `devices/` — device card, status badge, operating mode toggle
- `telemetry/` — Recharts-based sensor history chart
- `rules/` — rule creation/listing forms
- `admin/` — admin-specific views
- `ui/` — generic reusable primitives (buttons, modals, etc.)

---

## Simulator Architecture

`DigitalTwinManager` — single class orchestrating all simulation:
- **Device Registry Sync:** Polls PostgreSQL every `SYNC_SECONDS` (default 15s) + LISTEN/NOTIFY event-driven re-sync
- **Per-Device Runtime:** Each device runs in its own daemon `Thread` (`DeviceRuntime`)
- **Simulation Patterns:** `random_walk`, `constant`, `sine`, `actuator_state`
- **Profile Layering:** `defaults` → `metrics[METRIC_TYPE]` → `devices[device_id]` → `devices[feed:{feed_key}]` (cascading override)
- **Actuator Command Reception:** Subscribes to each actuator feed; translates `ON/1/OFF/0/SET:{value}` commands

---

## Roles

| Role | Access |
|---|---|
| `FARMER` | Own farms, devices, rules, telemetry, AI analysis |
| `ADMIN` | All farmers, all devices (approval), device models, system stats |
