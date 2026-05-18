<!-- refreshed: 2026-05-18 -->
# Architecture

**Analysis Date:** 2026-05-18

## System Overview

```text
┌─────────────────────────────────────────────────────────────┐
│                    User-Facing Web Layer                     │
├──────────────────┬──────────────────┬───────────────────────┤
│  React Router UI │  Zustand Stores  │  HTTP/STOMP Clients   │
│ `frontend/src`   │ `frontend/src/   │ `frontend/src/lib/`   │
│                  │  stores/`        │                       │
└────────┬─────────┴────────┬─────────┴──────────┬────────────┘
         │                  │                     │
         │ HTTP `/api/v1`   │ STOMP `/ws`         │ localStorage JWT
         ▼                  ▼                     ▼
┌─────────────────────────────────────────────────────────────┐
│                    Spring Boot API Layer                     │
│ `backend/api/src/main/java/com/yoloFarm/api/`                │
├──────────────────┬──────────────────┬───────────────────────┤
│ REST Controllers │ Service Layer    │ WebSocket/MQTT Layer  │
│ `controller/`    │ `service/`       │ `config/`, `service/  │
│                  │                  │  mqtt/`               │
└────────┬─────────┴────────┬─────────┴──────────┬────────────┘
         │                  │                     │
         │ JPA/JDBC         │ MQTT TLS            │ STOMP topics
         ▼                  ▼                     ▼
┌─────────────────────────────────────────────────────────────┐
│              Persistence, Broker, and Simulation             │
│ PostgreSQL + Flyway `backend/api/src/main/resources/db/`     │
│ Adafruit IO MQTT `backend/api/src/main/java/.../mqtt/`       │
│ Digital Twin `simulator/digital-twin/main.py`                │
└─────────────────────────────────────────────────────────────┘
```

## Component Responsibilities

| Component | Responsibility | File |
|-----------|----------------|------|
| React app root | Mounts the SPA and wraps rendering with an error boundary. | `frontend/src/main.tsx` |
| Route graph | Defines public, protected, farmer, admin, and profile routes. | `frontend/src/App.tsx` |
| API client | Centralizes Axios base URL, JWT attachment, 401 redirect, and API error extraction. | `frontend/src/lib/axios.ts` |
| WebSocket client | Owns STOMP/SockJS connections for farm telemetry, device status, admin stats, and unread notifications. | `frontend/src/lib/websocket.ts` |
| Auth store | Keeps JWT, role, profile, and authentication status in Zustand plus `localStorage`. | `frontend/src/stores/authStore.ts` |
| Notification store | Owns notification paging, unread count, mark-read, and read-all client state. | `frontend/src/stores/notificationStore.ts` |
| Spring entry point | Starts the API, scheduling, and async support. | `backend/api/src/main/java/com/yoloFarm/api/ApiApplication.java` |
| REST controllers | Convert authenticated HTTP requests into service calls and return DTO responses. | `backend/api/src/main/java/com/yoloFarm/api/controller/` |
| Service layer | Holds business workflows for auth, farms, devices, telemetry, rules, notifications, admin, and automation. | `backend/api/src/main/java/com/yoloFarm/api/service/` |
| Repository layer | Encapsulates JPA queries and projections for entities. | `backend/api/src/main/java/com/yoloFarm/api/repository/` |
| MQTT receiver | Subscribes to Adafruit feeds, maps feed keys to devices, updates connection status, and notifies telemetry observers. | `backend/api/src/main/java/com/yoloFarm/api/service/mqtt/MqttReceiverService.java` |
| Telemetry observers | Persist telemetry, run rules, and push realtime messages from the MQTT event. | `backend/api/src/main/java/com/yoloFarm/api/service/mqtt/observer/` |
| Irrigation strategies | Enforce manual, threshold-auto, and scheduled control modes before publishing commands. | `backend/api/src/main/java/com/yoloFarm/api/service/strategy/` |
| Security layer | Applies stateless JWT auth, admin route authorization, REST auth errors, rate limiting, and WebSocket auth. | `backend/api/src/main/java/com/yoloFarm/api/security/` |
| Digital twin simulator | Polls/listens for active devices, runs per-device simulation threads, and publishes/subscribes Adafruit MQTT. | `simulator/digital-twin/main.py` |

## Pattern Overview

**Overall:** Multi-runtime smart-farm system with a layered Spring backend, route-driven React SPA, and event-driven telemetry pipeline.

**Key Characteristics:**
- Backend code is organized by technical layer under `backend/api/src/main/java/com/yoloFarm/api/`: `controller`, `service`, `repository`, `entity`, `dto`, `config`, `security`, and `exception`.
- Frontend pages compose domain components and call the API directly through `frontend/src/lib/axios.ts`; cross-page client state is limited to Zustand stores in `frontend/src/stores/`.
- Realtime telemetry uses an observer pattern: `MqttReceiverService` receives MQTT, creates `SensorData`, and fans out to `DatabaseLoggerObserver`, `RuleEngineObserver`, and `WebSocketNotifierObserver`.
- Device control uses a strategy pattern: callers pass `ManualStrategy`, `AutoThresholdStrategy`, or `ScheduledStrategy` to `IrrigationContext.executeControl(...)`.
- The simulator is a separate Python process that reads PostgreSQL state, reacts to `LISTEN device_events`, and publishes simulated telemetry to Adafruit IO.

## Layers

**Frontend Routing and Layout:**
- Purpose: Define navigation, route protection, role boundaries, and the shared application frame.
- Location: `frontend/src/App.tsx`, `frontend/src/components/guards/`, `frontend/src/components/layout/`
- Contains: React Router routes, `ProtectedRoute`, `PublicRoute`, `MainLayout`, sidebar/header components.
- Depends on: `frontend/src/stores/authStore.ts`, page components under `frontend/src/pages/`.
- Used by: Browser users through `frontend/src/main.tsx`.

**Frontend Pages and Components:**
- Purpose: Implement admin and farmer workflows and reusable UI/domain widgets.
- Location: `frontend/src/pages/`, `frontend/src/components/`
- Contains: Page-level data loading in files such as `frontend/src/pages/farmer/FarmDetailPage.tsx`; domain tabs/cards/modals in `frontend/src/components/devices/`, `frontend/src/components/rules/`, `frontend/src/components/telemetry/`, `frontend/src/components/admin/`, and `frontend/src/components/farms/`.
- Depends on: `frontend/src/lib/axios.ts`, `frontend/src/lib/websocket.ts`, `frontend/src/types/index.ts`.
- Used by: Routes in `frontend/src/App.tsx`.

**Frontend Client State and IO:**
- Purpose: Hold cross-route state and centralize backend communication.
- Location: `frontend/src/stores/`, `frontend/src/lib/`
- Contains: Zustand stores, Axios singleton, STOMP/SockJS connection helpers.
- Depends on: `localStorage`, `/api/v1` HTTP endpoints, `/ws` STOMP endpoint.
- Used by: Pages, layout, route guards, and domain components.

**Backend HTTP API:**
- Purpose: Expose authenticated REST endpoints under `/api/v1`.
- Location: `backend/api/src/main/java/com/yoloFarm/api/controller/`
- Contains: `AuthController`, `FarmController`, `DeviceController`, `RuleController`, `NotificationController`, `AdminController`, `DeviceModelController`, `UserController`.
- Depends on: Service beans, request DTOs in `backend/api/src/main/java/com/yoloFarm/api/dto/request/`, authenticated `User` principals.
- Used by: Frontend Axios client and API contract `backend/api/openAPI.yaml`.

**Backend Business Services:**
- Purpose: Enforce ownership, status transitions, feed-key constraints, rule automation, notification semantics, and DTO mapping.
- Location: `backend/api/src/main/java/com/yoloFarm/api/service/`
- Contains: Domain services plus subpackages `automation/`, `impl/`, `mqtt/`, `security/`, and `strategy/`.
- Depends on: Repositories, JPA entities, `JdbcTemplate`, MQTT sender/receiver, notification service, Adafruit API service.
- Used by: REST controllers, MQTT observers, schedulers, and other services.

**Backend Persistence:**
- Purpose: Store users, farms, models, devices, notifications, rules, and telemetry.
- Location: `backend/api/src/main/java/com/yoloFarm/api/entity/`, `backend/api/src/main/java/com/yoloFarm/api/repository/`, `backend/api/src/main/resources/db/migration/`
- Contains: JPA entities, Spring Data repositories, admin projections, Flyway migrations.
- Depends on: PostgreSQL in production/local runtime; H2 for tests.
- Used by: Service layer, MQTT receiver, telemetry observers, simulator SQL queries.

**Backend Realtime and MQTT:**
- Purpose: Bridge Adafruit IO MQTT to database, automation, and frontend WebSocket topics.
- Location: `backend/api/src/main/java/com/yoloFarm/api/config/WebSocketConfig.java`, `backend/api/src/main/java/com/yoloFarm/api/config/MqttConfig.java`, `backend/api/src/main/java/com/yoloFarm/api/service/mqtt/`
- Contains: MQTT sender/receiver, observer interfaces, observer implementations, STOMP broker configuration.
- Depends on: Paho MQTT client, `SimpMessagingTemplate`, repositories, `JdbcTemplate`.
- Used by: Device control strategies, telemetry pipeline, frontend STOMP clients.

**Backend Security and Error Handling:**
- Purpose: Apply stateless JWT authentication, authorization, WebSocket authentication, rate limiting, and uniform REST errors.
- Location: `backend/api/src/main/java/com/yoloFarm/api/security/`, `backend/api/src/main/java/com/yoloFarm/api/config/SecurityConfig.java`, `backend/api/src/main/java/com/yoloFarm/api/exception/`
- Contains: `JwtAuthenticationFilter`, `RateLimitFilter`, REST auth handlers, `WebSocketAuthChannelInterceptor`, `GlobalExceptionHandler`.
- Depends on: `JwtService`, `AuthenticationProvider`, Spring Security.
- Used by: HTTP filter chain, STOMP inbound channel, controllers.

**Digital Twin Runtime:**
- Purpose: Simulate active devices and actuator state using DB metadata and Adafruit MQTT.
- Location: `simulator/digital-twin/main.py`, `simulator/digital-twin/profiles.example.json`, `simulator/digital-twin/tools/`
- Contains: `DigitalTwinManager`, device runtime dataclasses, feed-key tooling, E2E provisioning helper.
- Depends on: PostgreSQL, Adafruit MQTT credentials, `profiles.json`.
- Used by: Local/development simulation via `scripts/run-simulator.ps1` and `simulator/digital-twin/scripts/run-simulator.ps1`.

## Data Flow

### Primary HTTP Request Path

1. React route renders a page from `frontend/src/App.tsx:56` and pages call the Axios singleton from `frontend/src/lib/axios.ts:4`.
2. Axios attaches the JWT from `localStorage` in `frontend/src/lib/axios.ts:12` and sends requests to `/api/v1`.
3. `SecurityConfig.securityFilterChain(...)` applies JWT auth and route authorization in `backend/api/src/main/java/com/yoloFarm/api/config/SecurityConfig.java:32`.
4. A controller accepts the authenticated request, for example `DeviceController.sendCommand(...)` in `backend/api/src/main/java/com/yoloFarm/api/controller/DeviceController.java:78`.
5. The controller delegates to services such as `DeviceService` and `IrrigationContext` in `backend/api/src/main/java/com/yoloFarm/api/service/DeviceService.java` and `backend/api/src/main/java/com/yoloFarm/api/service/strategy/IrrigationContext.java:7`.
6. Services use repositories under `backend/api/src/main/java/com/yoloFarm/api/repository/` and return DTOs from `backend/api/src/main/java/com/yoloFarm/api/dto/response/`.
7. Exceptions are normalized by `GlobalExceptionHandler` in `backend/api/src/main/java/com/yoloFarm/api/exception/GlobalExceptionHandler.java:22`.

### Telemetry Ingestion and Realtime Flow

1. Simulator or physical devices publish values to Adafruit MQTT; the Python simulator publishes in `simulator/digital-twin/main.py:139`.
2. `MqttReceiverService` subscribes to the Adafruit wildcard feed topic in `backend/api/src/main/java/com/yoloFarm/api/service/mqtt/MqttReceiverService.java:130`.
3. Incoming MQTT messages are parsed and matched to devices in `backend/api/src/main/java/com/yoloFarm/api/service/mqtt/MqttReceiverService.java:180`.
4. The receiver updates connection status and builds a `SensorData` event before calling `notifyObservers(...)` in `backend/api/src/main/java/com/yoloFarm/api/service/mqtt/MqttReceiverService.java:249`.
5. `DatabaseLoggerObserver` persists telemetry in `backend/api/src/main/java/com/yoloFarm/api/service/mqtt/observer/DatabaseLoggerObserver.java:13`.
6. `RuleEngineObserver` evaluates active rules and may execute `AutoThresholdStrategy` in `backend/api/src/main/java/com/yoloFarm/api/service/mqtt/observer/RuleEngineObserver.java:22`.
7. `WebSocketNotifierObserver` pushes `/topic/farm/{farmId}/telemetry` in `backend/api/src/main/java/com/yoloFarm/api/service/mqtt/observer/WebSocketNotifierObserver.java:15`.
8. Frontend domain components subscribe through `connectToFarm(...)` in `frontend/src/lib/websocket.ts:43`; `OverviewTab` starts the farm connection in `frontend/src/components/devices/OverviewTab.tsx:55`.

### Device Command Flow

1. A frontend component posts a command through `frontend/src/lib/axios.ts:4`.
2. `DeviceController.sendCommand(...)` validates ownership and fetches farm ID in `backend/api/src/main/java/com/yoloFarm/api/controller/DeviceController.java:78`.
3. `IrrigationContext.executeControl(...)` receives `ManualStrategy` in `backend/api/src/main/java/com/yoloFarm/api/service/strategy/IrrigationContext.java:7`.
4. `ManualStrategy` enforces `OperatingModeEnum.MANUAL`, publishes via `MqttSenderService`, then updates `Device.isActive` in `backend/api/src/main/java/com/yoloFarm/api/service/strategy/ManualStrategy.java:16`.
5. The simulator receives actuator commands in `simulator/digital-twin/main.py:258` and updates runtime actuator state.

### Device Approval to Simulator Flow

1. Admin approval runs through `DeviceService.approveDevice(...)` in `backend/api/src/main/java/com/yoloFarm/api/service/DeviceService.java`.
2. Device approval creates/assigns an Adafruit feed and executes `NOTIFY device_events, 'approve'` in `backend/api/src/main/java/com/yoloFarm/api/service/DeviceService.java:244`.
3. The simulator listens with `LISTEN device_events` in `simulator/digital-twin/main.py:305`.
4. `DigitalTwinManager._sync_once()` reloads active devices and starts/stops per-device runtime threads in `simulator/digital-twin/main.py:221`.

**State Management:**
- Backend authoritative state is PostgreSQL, represented by entities in `backend/api/src/main/java/com/yoloFarm/api/entity/` and schema migrations in `backend/api/src/main/resources/db/migration/`.
- Frontend durable auth state is `localStorage` and volatile UI state is React component state plus Zustand stores in `frontend/src/stores/`.
- Realtime connection singletons are module-level STOMP clients in `frontend/src/lib/websocket.ts`.
- MQTT receiver caches feed keys in a `ConcurrentHashMap` in `backend/api/src/main/java/com/yoloFarm/api/service/mqtt/MqttReceiverService.java`.
- Simulator runtime state is an in-memory `dict` of `DeviceRuntime` guarded by an `RLock` in `simulator/digital-twin/main.py`.

## Key Abstractions

**DTO Boundary:**
- Purpose: Keep REST request/response payloads separate from JPA entities.
- Examples: `backend/api/src/main/java/com/yoloFarm/api/dto/request/DeviceRequest.java`, `backend/api/src/main/java/com/yoloFarm/api/dto/response/DeviceResponse.java`, `backend/api/src/main/java/com/yoloFarm/api/dto/response/ErrorResponse.java`
- Pattern: Controllers receive request DTOs and services map entities to response DTOs.

**Repositories and Projections:**
- Purpose: Centralize database access and read-optimized admin projections.
- Examples: `backend/api/src/main/java/com/yoloFarm/api/repository/DeviceRepository.java`, `backend/api/src/main/java/com/yoloFarm/api/repository/projection/AdminDeviceProjection.java`
- Pattern: Services call Spring Data repository methods; repositories own query naming and projection return types.

**Observer Pattern for Telemetry:**
- Purpose: Decouple MQTT ingestion from persistence, automation, and WebSocket notification.
- Examples: `backend/api/src/main/java/com/yoloFarm/api/service/mqtt/observer/Subject.java`, `backend/api/src/main/java/com/yoloFarm/api/service/mqtt/observer/Observer.java`, `backend/api/src/main/java/com/yoloFarm/api/service/mqtt/MqttReceiverService.java`
- Pattern: Add new telemetry side effects as `Observer` components instead of editing unrelated observers.

**Strategy Pattern for Irrigation Control:**
- Purpose: Select execution rules by control mode without sharing mutable strategy state.
- Examples: `backend/api/src/main/java/com/yoloFarm/api/service/strategy/IrrigationStrategy.java`, `backend/api/src/main/java/com/yoloFarm/api/service/strategy/IrrigationContext.java`, `backend/api/src/main/java/com/yoloFarm/api/service/strategy/ManualStrategy.java`
- Pattern: Callers pass the concrete strategy bean into `IrrigationContext.executeControl(...)`.

**JWT Security Principal:**
- Purpose: Make the authenticated `User` available to controllers through `@AuthenticationPrincipal`.
- Examples: `backend/api/src/main/java/com/yoloFarm/api/security/JwtAuthenticationFilter.java`, `backend/api/src/main/java/com/yoloFarm/api/controller/DeviceController.java`
- Pattern: Controllers use `currentUser.getId()` and services enforce ownership.

**WebSocket Topic Contract:**
- Purpose: Provide push updates without frontend polling for telemetry, status, admin stats, and notification counts.
- Examples: `backend/api/src/main/java/com/yoloFarm/api/config/WebSocketConfig.java`, `frontend/src/lib/websocket.ts`
- Pattern: Backend publishes to `/topic/...` or `/user/queue/...`; frontend connects with JWT STOMP headers.

**Digital Twin Manager:**
- Purpose: Model active devices as independent runtimes synchronized from database state.
- Examples: `simulator/digital-twin/main.py`
- Pattern: Fetch DB state, merge profiles, start/stop daemon threads, publish telemetry, update actuator state from MQTT.

## Entry Points

**Backend API:**
- Location: `backend/api/src/main/java/com/yoloFarm/api/ApiApplication.java`
- Triggers: `scripts/run-backend.ps1`, Maven Spring Boot plugin, Dockerfile runtime.
- Responsibilities: Boot Spring context, enable scheduling, enable async support.

**Frontend SPA:**
- Location: `frontend/src/main.tsx`
- Triggers: Vite dev server or built `frontend/index.html`.
- Responsibilities: Mount `App`, install global rendering error boundary, load CSS.

**Frontend Route Graph:**
- Location: `frontend/src/App.tsx`
- Triggers: Browser navigation.
- Responsibilities: Load auth from storage, fetch profile, define role-gated routes, mount toast system.

**Digital Twin Simulator:**
- Location: `simulator/digital-twin/main.py`
- Triggers: `scripts/run-simulator.ps1`, `simulator/digital-twin/scripts/run-simulator.ps1`, or direct Python execution.
- Responsibilities: Validate env vars, connect MQTT, listen for PostgreSQL notifications, manage runtime threads.

**Database Schema:**
- Location: `backend/api/src/main/resources/db/migration/V1__init_schema.sql`
- Triggers: Flyway on backend startup.
- Responsibilities: Create application tables and foreign keys.

**API Contract:**
- Location: `backend/api/openAPI.yaml`
- Triggers: Human/API client reference.
- Responsibilities: Document HTTP API surface.

## Architectural Constraints

- **Threading:** Backend HTTP handling uses Spring request threads; MQTT observer fan-out uses a bounded `ThreadPoolExecutor` in `backend/api/src/main/java/com/yoloFarm/api/service/mqtt/MqttReceiverService.java`; schedulers are enabled by `ApiApplication`. The simulator runs one daemon thread per active device in `simulator/digital-twin/main.py`.
- **Global state:** Frontend STOMP clients are module-level variables in `frontend/src/lib/websocket.ts`; simulator runtimes are manager-level mutable state in `simulator/digital-twin/main.py`; MQTT feed cache is module/service state in `backend/api/src/main/java/com/yoloFarm/api/service/mqtt/MqttReceiverService.java`.
- **Circular imports:** No TypeScript circular import chain is detected from sampled source. Backend uses `@Lazy` for `MqttReceiverService` in `DeviceService`, which indicates a Spring bean dependency cycle around device approval/cache interactions.
- **Persistence contract:** Backend schema is Flyway-managed in `backend/api/src/main/resources/db/migration/`, but `spring.jpa.hibernate.ddl-auto` is set in `backend/api/src/main/resources/application.yml`; schema-changing work must update migrations, not rely on runtime Hibernate mutation.
- **Authentication contract:** HTTP auth is stateless JWT through `SecurityConfig`; WebSocket auth is intercepted by `WebSocketAuthChannelInterceptor`; frontend clients must pass the same stored JWT.
- **External broker contract:** Device telemetry and commands are coupled to Adafruit feed keys stored on `Device.adafruitFeedKey`.

## Anti-Patterns

### Adding Domain Logic in Controllers

**What happens:** Controllers should coordinate request validation and call services, but `DeviceController.sendCommand(...)` performs ownership, farm lookup, strategy selection, and error mapping around command execution in `backend/api/src/main/java/com/yoloFarm/api/controller/DeviceController.java:78`.
**Why it's wrong:** It spreads command workflow rules outside the service layer and makes it harder to reuse command behavior from automation or scheduled flows.
**Do this instead:** Put new command workflows in services under `backend/api/src/main/java/com/yoloFarm/api/service/` and let controllers delegate to a single service method.

### Direct JDBC Updates Beside JPA Entities

**What happens:** `MqttReceiverService` uses `JdbcTemplate` for device online status updates while repositories/JPA own most persistence in `backend/api/src/main/java/com/yoloFarm/api/service/mqtt/MqttReceiverService.java:180`.
**Why it's wrong:** Mixed persistence paths can bypass entity lifecycle assumptions and make transactional behavior harder to reason about.
**Do this instead:** Use repository methods for normal domain writes; reserve `JdbcTemplate` for deliberate low-level operations such as PostgreSQL `NOTIFY` in `backend/api/src/main/java/com/yoloFarm/api/service/DeviceService.java:244` and document the reason in the service.

### Page-Level API Sprawl

**What happens:** Page components such as `frontend/src/pages/farmer/FarmDetailPage.tsx:26` issue multiple API calls and perform response enrichment locally.
**Why it's wrong:** Repeated API orchestration can diverge across pages and makes state refresh behavior inconsistent.
**Do this instead:** Place shared API orchestration in `frontend/src/lib/` or a dedicated store under `frontend/src/stores/` when more than one page/component needs the same workflow.

### Multiple WebSocket Singletons in One Module

**What happens:** `frontend/src/lib/websocket.ts` keeps separate module-level STOMP clients for farm, admin stats, and notification unread connections.
**Why it's wrong:** Adding more channels increases lifecycle complexity and risks unclosed connections when components remount.
**Do this instead:** Add channel-specific connect/disconnect helpers only when the lifecycle is clear; keep cleanup colocated with the component using the helper, as in `frontend/src/components/layout/TopHeader.tsx`.

## Error Handling

**Strategy:** Backend services throw typed Spring/security/domain exceptions and `GlobalExceptionHandler` converts them into `ErrorResponse`; frontend Axios helpers extract `details` or `message` for user display.

**Patterns:**
- Use `EntityNotFoundException` for missing resources and let `GlobalExceptionHandler` return 404.
- Use `AccessDeniedException` for ownership/role violations and let Spring/global handlers return 403.
- Use `ConflictException` for invalid state transitions or uniqueness conflicts.
- Use `IllegalStateException` for invalid business state, invalid aggregate values, or command rejection.
- In MQTT observer side effects, catch and log exceptions inside each observer so one side effect does not stop the fan-out.
- In frontend API calls, use `getApiErrorMessage(...)` from `frontend/src/lib/axios.ts` and show errors through toast/UI state.

## Cross-Cutting Concerns

**Logging:** Backend uses Lombok `@Slf4j` in services, observers, and handlers. Simulator uses Python `logging` configured in `simulator/digital-twin/main.py`. Frontend uses `console` for WebSocket and notification-store diagnostics.

**Validation:** Backend request DTOs use Jakarta validation and controllers annotate request bodies with `@Valid`. Additional business validation lives in services such as `DeviceService` and `TelemetryService`.

**Authentication:** Backend HTTP uses JWT via `JwtAuthenticationFilter`; WebSocket uses `WebSocketAuthChannelInterceptor`; frontend stores the token in `localStorage` and attaches it in Axios/STOMP clients.

**Authorization:** Admin routes are constrained by `SecurityConfig`; domain ownership checks are performed in service/repository queries such as `findByIdAndFarmOwnerId(...)`.

**Scheduling:** Backend scheduled services include device heartbeat, rule scheduling, and auto-irrigation safety under `backend/api/src/main/java/com/yoloFarm/api/service/` and `backend/api/src/main/java/com/yoloFarm/api/service/automation/`.

**Configuration:** Backend reads environment-backed properties from `backend/api/src/main/resources/application.yml`; frontend uses Vite env fallbacks in `frontend/src/lib/axios.ts` and `frontend/src/lib/websocket.ts`; simulator reads `.env` through `python-dotenv` in `simulator/digital-twin/main.py`.

**Project workflow constraints:** Local GSD skills under `.codex/skills/` define planning and execution artifacts under `.planning/`; codebase maps belong in `.planning/codebase/` and should preserve file-path-heavy, prescriptive guidance for downstream commands.

---

*Architecture analysis: 2026-05-18*
