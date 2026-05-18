# Codebase Structure

**Analysis Date:** 2026-05-18

## Directory Layout

```text
YoloFarm/
├── .codex/                 # Local Codex/GSD skills and workflow support
├── .github/                # GitHub metadata/workflows when present
├── .planning/              # GSD planning state, roadmap, phases, and codebase maps
├── backend/
│   └── api/                # Spring Boot API, OpenAPI contract, database migrations, backend tests
├── docs/                   # Human-facing project documentation
├── frontend/               # React + Vite + TypeScript SPA
├── plantUML/               # Architecture/design diagrams
├── scripts/                # Root PowerShell scripts for local backend/frontend/simulator operations
├── simulator/
│   └── digital-twin/       # Python digital twin simulator and helper tooling
├── README.md               # Root setup and system overview
├── KNOWLEDGE_BASE.md       # Project knowledge notes
└── implementation_plan.md  # Implementation planning notes
```

## Directory Purposes

**`.planning/`:**
- Purpose: Stores GSD project state and generated planning/reference artifacts.
- Contains: Codebase maps in `.planning/codebase/`, roadmap/state files, phase artifacts.
- Key files: `.planning/codebase/ARCHITECTURE.md`, `.planning/codebase/STRUCTURE.md`

**`.codex/skills/`:**
- Purpose: Local GSD command skills and workflow adapters used by Codex.
- Contains: One directory per skill, each with a `SKILL.md` index and optional scripts/templates/rules.
- Key files: `.codex/skills/gsd-map-codebase/SKILL.md`, `.codex/skills/gsd-plan-phase/SKILL.md`, `.codex/skills/gsd-execute-phase/SKILL.md`

**`backend/api/`:**
- Purpose: Java Spring Boot backend service.
- Contains: Maven wrapper, `pom.xml`, Dockerfile, OpenAPI contract, Java source, resources, tests, SQL.
- Key files: `backend/api/pom.xml`, `backend/api/openAPI.yaml`, `backend/api/Dockerfile`, `backend/api/src/main/java/com/yoloFarm/api/ApiApplication.java`

**`backend/api/src/main/java/com/yoloFarm/api/`:**
- Purpose: Main backend application package.
- Contains: Package-by-layer backend code.
- Key files: `backend/api/src/main/java/com/yoloFarm/api/ApiApplication.java`

**`backend/api/src/main/java/com/yoloFarm/api/config/`:**
- Purpose: Spring bean and platform configuration.
- Contains: Security, CORS, MQTT, WebSocket, app initializer, clock/application beans.
- Key files: `backend/api/src/main/java/com/yoloFarm/api/config/SecurityConfig.java`, `backend/api/src/main/java/com/yoloFarm/api/config/WebSocketConfig.java`, `backend/api/src/main/java/com/yoloFarm/api/config/MqttConfig.java`

**`backend/api/src/main/java/com/yoloFarm/api/controller/`:**
- Purpose: REST API endpoints under `/api/v1`.
- Contains: Auth, user, farm, device, device model, rule, notification, and admin controllers.
- Key files: `backend/api/src/main/java/com/yoloFarm/api/controller/DeviceController.java`, `backend/api/src/main/java/com/yoloFarm/api/controller/AuthController.java`, `backend/api/src/main/java/com/yoloFarm/api/controller/AdminController.java`

**`backend/api/src/main/java/com/yoloFarm/api/dto/`:**
- Purpose: API request/response payloads and internal event records.
- Contains: `request/`, `response/`, and `SensorData.java`.
- Key files: `backend/api/src/main/java/com/yoloFarm/api/dto/SensorData.java`, `backend/api/src/main/java/com/yoloFarm/api/dto/request/DeviceCommandRequest.java`, `backend/api/src/main/java/com/yoloFarm/api/dto/response/DeviceResponse.java`

**`backend/api/src/main/java/com/yoloFarm/api/entity/`:**
- Purpose: JPA persistence model.
- Contains: Entities for users, farms, device models, devices, notifications, rules, telemetry.
- Key files: `backend/api/src/main/java/com/yoloFarm/api/entity/Device.java`, `backend/api/src/main/java/com/yoloFarm/api/entity/Farm.java`, `backend/api/src/main/java/com/yoloFarm/api/entity/Rule.java`

**`backend/api/src/main/java/com/yoloFarm/api/enums/`:**
- Purpose: Shared domain enumerations.
- Contains: Device status/type, metric type, operating mode, role, rule type, commands, connection status.
- Key files: `backend/api/src/main/java/com/yoloFarm/api/enums/OperatingModeEnum.java`, `backend/api/src/main/java/com/yoloFarm/api/enums/RoleEnum.java`

**`backend/api/src/main/java/com/yoloFarm/api/exception/`:**
- Purpose: Domain exceptions and global REST error mapping.
- Contains: `ConflictException` and `GlobalExceptionHandler`.
- Key files: `backend/api/src/main/java/com/yoloFarm/api/exception/GlobalExceptionHandler.java`

**`backend/api/src/main/java/com/yoloFarm/api/repository/`:**
- Purpose: Spring Data repositories and query projections.
- Contains: Entity repositories and `projection/` DTO interfaces/classes for admin views.
- Key files: `backend/api/src/main/java/com/yoloFarm/api/repository/DeviceRepository.java`, `backend/api/src/main/java/com/yoloFarm/api/repository/RuleRepository.java`, `backend/api/src/main/java/com/yoloFarm/api/repository/projection/AdminDeviceProjection.java`

**`backend/api/src/main/java/com/yoloFarm/api/security/`:**
- Purpose: HTTP and WebSocket security infrastructure.
- Contains: JWT filter, rate limit filter, REST auth handlers, WebSocket inbound auth interceptor.
- Key files: `backend/api/src/main/java/com/yoloFarm/api/security/JwtAuthenticationFilter.java`, `backend/api/src/main/java/com/yoloFarm/api/security/WebSocketAuthChannelInterceptor.java`, `backend/api/src/main/java/com/yoloFarm/api/security/RateLimitFilter.java`

**`backend/api/src/main/java/com/yoloFarm/api/service/`:**
- Purpose: Business workflows and integration services.
- Contains: Domain services plus `automation/`, `impl/`, `mqtt/`, `security/`, and `strategy/` subpackages.
- Key files: `backend/api/src/main/java/com/yoloFarm/api/service/DeviceService.java`, `backend/api/src/main/java/com/yoloFarm/api/service/TelemetryService.java`, `backend/api/src/main/java/com/yoloFarm/api/service/RuleService.java`, `backend/api/src/main/java/com/yoloFarm/api/service/NotificationService.java`

**`backend/api/src/main/java/com/yoloFarm/api/service/automation/`:**
- Purpose: Runtime automation safety and cooldown state.
- Contains: Auto-irrigation safety and automation runtime state services.
- Key files: `backend/api/src/main/java/com/yoloFarm/api/service/automation/AutoIrrigationSafetyService.java`, `backend/api/src/main/java/com/yoloFarm/api/service/automation/AutomationRuntimeStateService.java`

**`backend/api/src/main/java/com/yoloFarm/api/service/impl/`:**
- Purpose: Implementations for service interfaces/integrations.
- Contains: Adafruit API and MQTT sender implementations, auth service implementation.
- Key files: `backend/api/src/main/java/com/yoloFarm/api/service/impl/AdafruitApiServiceImpl.java`, `backend/api/src/main/java/com/yoloFarm/api/service/impl/MqttSenderServiceImpl.java`, `backend/api/src/main/java/com/yoloFarm/api/service/impl/AuthServiceImpl.java`

**`backend/api/src/main/java/com/yoloFarm/api/service/mqtt/`:**
- Purpose: MQTT ingress/egress and telemetry observer infrastructure.
- Contains: MQTT receiver, sender interface, observer package.
- Key files: `backend/api/src/main/java/com/yoloFarm/api/service/mqtt/MqttReceiverService.java`, `backend/api/src/main/java/com/yoloFarm/api/service/mqtt/MqttSenderService.java`

**`backend/api/src/main/java/com/yoloFarm/api/service/mqtt/observer/`:**
- Purpose: Side effects triggered by telemetry events.
- Contains: Observer/Subject contracts plus database logger, rule engine, and WebSocket notifier observers.
- Key files: `backend/api/src/main/java/com/yoloFarm/api/service/mqtt/observer/Observer.java`, `backend/api/src/main/java/com/yoloFarm/api/service/mqtt/observer/DatabaseLoggerObserver.java`, `backend/api/src/main/java/com/yoloFarm/api/service/mqtt/observer/RuleEngineObserver.java`, `backend/api/src/main/java/com/yoloFarm/api/service/mqtt/observer/WebSocketNotifierObserver.java`

**`backend/api/src/main/java/com/yoloFarm/api/service/security/`:**
- Purpose: Security support services.
- Contains: JWT service.
- Key files: `backend/api/src/main/java/com/yoloFarm/api/service/security/JwtService.java`

**`backend/api/src/main/java/com/yoloFarm/api/service/strategy/`:**
- Purpose: Device/irrigation command strategy implementations.
- Contains: Strategy interface, context, manual, auto-threshold, and scheduled strategies.
- Key files: `backend/api/src/main/java/com/yoloFarm/api/service/strategy/IrrigationStrategy.java`, `backend/api/src/main/java/com/yoloFarm/api/service/strategy/IrrigationContext.java`, `backend/api/src/main/java/com/yoloFarm/api/service/strategy/ManualStrategy.java`

**`backend/api/src/main/resources/`:**
- Purpose: Runtime backend resources and configuration.
- Contains: Spring configuration, Flyway migrations, static/template placeholders.
- Key files: `backend/api/src/main/resources/application.yml`, `backend/api/src/main/resources/application.properties`, `backend/api/src/main/resources/db/migration/V1__init_schema.sql`

**`backend/api/src/test/`:**
- Purpose: Backend unit/integration tests and test configuration.
- Contains: Java tests under `src/test/java` and test `application.yml`.
- Key files: `backend/api/src/test/resources/application.yml`, `backend/api/src/test/java/com/yoloFarm/api/FarmCrudIntegrationTest.java`, `backend/api/src/test/java/com/yoloFarm/api/MqttReceiverServiceTest.java`

**`frontend/`:**
- Purpose: React frontend application.
- Contains: npm package files, Vite/Tailwind/TypeScript config, static HTML, source code, Vercel config.
- Key files: `frontend/package.json`, `frontend/vite.config.ts`, `frontend/tailwind.config.ts`, `frontend/tsconfig.json`, `frontend/index.html`

**`frontend/src/`:**
- Purpose: Frontend application source.
- Contains: App root, pages, components, stores, lib clients, types, CSS, tests.
- Key files: `frontend/src/main.tsx`, `frontend/src/App.tsx`, `frontend/src/index.css`

**`frontend/src/components/`:**
- Purpose: Reusable React components grouped by domain.
- Contains: `admin/`, `devices/`, `farms/`, `guards/`, `layout/`, `rules/`, `telemetry/`, `ui/`.
- Key files: `frontend/src/components/layout/MainLayout.tsx`, `frontend/src/components/devices/OverviewTab.tsx`, `frontend/src/components/telemetry/TelemetryTab.tsx`, `frontend/src/components/ui/Modal.tsx`

**`frontend/src/pages/`:**
- Purpose: Route-level React screens.
- Contains: Admin pages, auth pages, farmer pages, profile page, page tests.
- Key files: `frontend/src/pages/farmer/FarmDetailPage.tsx`, `frontend/src/pages/admin/AdminDashboardPage.tsx`, `frontend/src/pages/auth/LoginPage.tsx`, `frontend/src/pages/ProfilePage.tsx`

**`frontend/src/lib/`:**
- Purpose: Browser-side backend clients and IO helpers.
- Contains: Axios singleton and WebSocket/STOMP connection helpers.
- Key files: `frontend/src/lib/axios.ts`, `frontend/src/lib/websocket.ts`

**`frontend/src/stores/`:**
- Purpose: Zustand cross-route state.
- Contains: Auth and notification stores plus store tests.
- Key files: `frontend/src/stores/authStore.ts`, `frontend/src/stores/notificationStore.ts`

**`frontend/src/types/`:**
- Purpose: Shared TypeScript API/domain types.
- Contains: Central type exports.
- Key files: `frontend/src/types/index.ts`

**`simulator/digital-twin/`:**
- Purpose: Python process that simulates sensors/actuators against the backend database and Adafruit MQTT.
- Contains: Main runtime, dependency list, profile example, setup docs, scripts, SQL, helper tools.
- Key files: `simulator/digital-twin/main.py`, `simulator/digital-twin/requirements.txt`, `simulator/digital-twin/profiles.example.json`, `simulator/digital-twin/README.md`

**`simulator/digital-twin/tools/`:**
- Purpose: Operational helper scripts for feed keys and E2E device verification.
- Contains: Python CLIs.
- Key files: `simulator/digital-twin/tools/feed_key_manager.py`, `simulator/digital-twin/tools/e2e_create_device_and_verify.py`

**`scripts/`:**
- Purpose: Root local-development automation.
- Contains: PowerShell scripts for starting/stopping/statusing local services.
- Key files: `scripts/run-backend.ps1`, `scripts/run-simulator.ps1`, `scripts/status-local.ps1`, `scripts/stop-all-local.ps1`

**`docs/`:**
- Purpose: Project documentation for humans and operators.
- Contains: API, architecture, configuration, deployment, development, testing, and project-detail docs.
- Key files: `docs/ARCHITECTURE.md`, `docs/API.md`, `docs/DEVELOPMENT.md`, `docs/TESTING.md`, `docs/CONFIGURATION.md`

**`plantUML/`:**
- Purpose: Diagram source files.
- Contains: Architecture and design-pattern PlantUML diagrams.
- Key files: `plantUML/architecture_system.puml`, `plantUML/design_pattern.puml`, `plantUML/design_pattern_backend.puml`

## Key File Locations

**Entry Points:**
- `backend/api/src/main/java/com/yoloFarm/api/ApiApplication.java`: Spring Boot backend startup.
- `frontend/src/main.tsx`: React DOM mount point and rendering error boundary.
- `frontend/src/App.tsx`: Browser routes, auth bootstrap, route protection, toast setup.
- `simulator/digital-twin/main.py`: Python digital twin process startup.

**Configuration:**
- `backend/api/pom.xml`: Backend Maven dependencies, Java version, Spring Boot parent, build plugins.
- `backend/api/src/main/resources/application.yml`: Backend datasource, JPA, Jackson, JWT, Adafruit MQTT, WebSocket, and automation properties.
- `backend/api/src/main/resources/application.properties`: Additional Spring resource configuration.
- `frontend/package.json`: Frontend npm scripts and dependencies.
- `frontend/vite.config.ts`: Vite dev/build configuration and proxy/path behavior.
- `frontend/tsconfig.json`: TypeScript compiler configuration.
- `frontend/tailwind.config.ts`: Tailwind design tokens/content scanning.
- `simulator/digital-twin/requirements.txt`: Python simulator dependencies.
- `simulator/digital-twin/profiles.example.json`: Example simulator profiles.

**Core Logic:**
- `backend/api/src/main/java/com/yoloFarm/api/service/DeviceService.java`: Device lifecycle, approval/removal, feed-key handling, ownership, mode/threshold updates.
- `backend/api/src/main/java/com/yoloFarm/api/service/RuleService.java`: Rule CRUD and rule validation.
- `backend/api/src/main/java/com/yoloFarm/api/service/TelemetryService.java`: Telemetry retrieval and aggregation.
- `backend/api/src/main/java/com/yoloFarm/api/service/mqtt/MqttReceiverService.java`: MQTT telemetry ingestion and observer dispatch.
- `backend/api/src/main/java/com/yoloFarm/api/service/mqtt/observer/RuleEngineObserver.java`: Sensor-triggered automation rules.
- `backend/api/src/main/java/com/yoloFarm/api/service/strategy/`: Command execution strategies.
- `frontend/src/pages/farmer/FarmDetailPage.tsx`: Farmer farm detail workflow and tab composition.
- `frontend/src/components/devices/OverviewTab.tsx`: Device overview and farm WebSocket status/telemetry wiring.
- `frontend/src/components/telemetry/TelemetryTab.tsx`: Telemetry charting workflow.
- `frontend/src/components/rules/RulesTab.tsx`: Rule management UI workflow.
- `simulator/digital-twin/main.py`: Simulator device discovery, threading, MQTT publish/subscribe, PostgreSQL listen loop.

**Testing:**
- `backend/api/src/test/java/com/yoloFarm/api/`: Backend tests.
- `backend/api/src/test/resources/application.yml`: Backend test configuration.
- `frontend/src/pages/__tests__/`: Frontend page tests.
- `frontend/src/stores/__tests__/`: Frontend store tests.
- `frontend/src/setupTests.ts`: Frontend test setup.

**Contracts and Migrations:**
- `backend/api/openAPI.yaml`: REST API contract.
- `backend/api/src/main/resources/db/migration/V1__init_schema.sql`: Initial database schema.
- `backend/api/src/main/resources/db/migration/V2__seed_data.sql`: Seed data.
- `backend/api/sql/seed_device_models.sql`: Device model seed helper.
- `simulator/digital-twin/sql/001_unique_adafruit_feed_key.sql`: Simulator/database operational SQL.

## Naming Conventions

**Files:**
- Backend classes use PascalCase Java filenames matching class/interface names: `DeviceService.java`, `DeviceController.java`, `GlobalExceptionHandler.java`.
- Backend request DTOs end in `Request`: `DeviceCommandRequest.java`, `RuleCreateRequest.java`.
- Backend response DTOs end in `Response` or describe a response payload: `DeviceResponse.java`, `AdminStatsResponse.java`, `UserProfile.java`.
- Backend repositories end in `Repository`: `DeviceRepository.java`, `TelemetryDataRepository.java`.
- Backend enums end in `Enum`: `OperatingModeEnum.java`, `RuleTypeEnum.java`.
- Frontend React components/pages use PascalCase `.tsx`: `FarmDetailPage.tsx`, `OverviewTab.tsx`, `Modal.tsx`.
- Frontend utility/store modules use camelCase `.ts`: `axios.ts`, `websocket.ts`, `authStore.ts`.
- Frontend tests use `.test.ts` or `.test.tsx`: `authStore.test.ts`, `FarmDetailPage.test.tsx`.
- Flyway migrations use `V{number}__description.sql`: `V1__init_schema.sql`, `V2__seed_data.sql`.
- Root/local scripts use kebab-case PowerShell names: `run-backend.ps1`, `status-local.ps1`.

**Directories:**
- Backend uses Java package-by-layer names: `controller`, `service`, `repository`, `entity`, `dto`, `config`, `security`, `exception`.
- Backend service subdomains use lower-case package names: `service/mqtt`, `service/strategy`, `service/automation`.
- Frontend groups by UI/application concern: `components`, `pages`, `stores`, `lib`, `types`.
- Frontend domain components use plural domain folder names: `components/devices`, `components/farms`, `components/rules`.
- Tests are either colocated in `__tests__` directories on frontend or Maven-standard `src/test/java` on backend.

## Where to Add New Code

**New Backend REST Feature:**
- Primary code: add or extend a controller in `backend/api/src/main/java/com/yoloFarm/api/controller/`.
- Business logic: add or extend a service in `backend/api/src/main/java/com/yoloFarm/api/service/`.
- Persistence: add repository methods in `backend/api/src/main/java/com/yoloFarm/api/repository/`.
- Payloads: add request DTOs in `backend/api/src/main/java/com/yoloFarm/api/dto/request/` and response DTOs in `backend/api/src/main/java/com/yoloFarm/api/dto/response/`.
- Tests: add backend tests under `backend/api/src/test/java/com/yoloFarm/api/`.
- Contract: update `backend/api/openAPI.yaml` when HTTP surface changes.

**New Backend Entity or Schema Change:**
- Entity: add/modify classes in `backend/api/src/main/java/com/yoloFarm/api/entity/`.
- Repository: add/modify interfaces in `backend/api/src/main/java/com/yoloFarm/api/repository/`.
- Enum: add shared enum values in `backend/api/src/main/java/com/yoloFarm/api/enums/`.
- Migration: add a new Flyway migration under `backend/api/src/main/resources/db/migration/`.
- Tests: add persistence/service integration tests under `backend/api/src/test/java/com/yoloFarm/api/`.

**New Telemetry Side Effect:**
- Primary code: add an `Observer` implementation in `backend/api/src/main/java/com/yoloFarm/api/service/mqtt/observer/`.
- Wiring: annotate it as a Spring component so `MqttReceiverService` auto-attaches it through injected observers.
- Tests: add tests under `backend/api/src/test/java/com/yoloFarm/api/service/mqtt/observer/` or `backend/api/src/test/java/com/yoloFarm/api/`.

**New Device Control Mode:**
- Primary code: add a strategy in `backend/api/src/main/java/com/yoloFarm/api/service/strategy/`.
- Shared contract: implement `IrrigationStrategy`.
- Caller integration: pass the strategy bean through `IrrigationContext.executeControl(...)`.
- Tests: add strategy tests under `backend/api/src/test/java/com/yoloFarm/api/`.

**New Frontend Page:**
- Primary code: add route-level screen under `frontend/src/pages/`.
- Route registration: add the route in `frontend/src/App.tsx`.
- Layout/guards: use `frontend/src/components/layout/` and `frontend/src/components/guards/`.
- Domain components: add reusable pieces under the relevant `frontend/src/components/{domain}/` folder.
- Types: update `frontend/src/types/index.ts`.
- Tests: add page tests under `frontend/src/pages/__tests__/`.

**New Frontend Domain Component:**
- Implementation: add `.tsx` files under `frontend/src/components/{domain}/`.
- Shared UI primitives: add only generic reusable widgets under `frontend/src/components/ui/`.
- API calls: use `frontend/src/lib/axios.ts` and shared helpers/stores when behavior is reused.
- Realtime: use or extend `frontend/src/lib/websocket.ts` and ensure calling components disconnect in cleanup.

**New Frontend Store:**
- Implementation: add a Zustand store under `frontend/src/stores/` with a `use{Name}Store` export.
- Tests: add store tests under `frontend/src/stores/__tests__/`.
- Persistence: use `localStorage` only for durable browser state such as auth/session preferences.

**New Simulator Behavior:**
- Runtime logic: edit `simulator/digital-twin/main.py`.
- Device profile defaults/examples: update `simulator/digital-twin/profiles.example.json`.
- Operational helpers: add scripts under `simulator/digital-twin/tools/`.
- Local script integration: update `scripts/run-simulator.ps1` or `simulator/digital-twin/scripts/run-simulator.ps1` when startup behavior changes.

**Utilities:**
- Backend shared helpers belong near their owning layer/package under `backend/api/src/main/java/com/yoloFarm/api/`; avoid root-level helper packages unless multiple domains use them.
- Frontend browser utilities belong in `frontend/src/lib/`.
- Root operational automation belongs in `scripts/`.

## Special Directories

**`.planning/`:**
- Purpose: GSD workflow state and generated planning artifacts.
- Generated: Yes
- Committed: Yes

**`.codex/skills/`:**
- Purpose: Local Codex/GSD skill definitions and workflow adapters.
- Generated: Yes
- Committed: Yes in this workspace

**`backend/api/src/main/resources/db/migration/`:**
- Purpose: Flyway database migrations.
- Generated: No
- Committed: Yes

**`frontend/node_modules/`:**
- Purpose: Installed npm dependencies.
- Generated: Yes
- Committed: No

**`frontend/dist/`:**
- Purpose: Vite production build output when generated.
- Generated: Yes
- Committed: No

**`frontend/src/**/__tests__/`:**
- Purpose: Frontend colocated tests.
- Generated: No
- Committed: Yes

**`backend/api/target/`:**
- Purpose: Maven build output when generated.
- Generated: Yes
- Committed: No

**`simulator/digital-twin/.venv/`:**
- Purpose: Python virtual environment when generated locally.
- Generated: Yes
- Committed: No

**`.env` and `.env.*`:**
- Purpose: Local environment configuration and secrets.
- Generated: No
- Committed: No; note existence only and do not read contents.

---

*Structure analysis: 2026-05-18*
