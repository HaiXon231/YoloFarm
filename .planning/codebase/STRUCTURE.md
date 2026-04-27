# STRUCTURE.md — Directory Layout

**Last mapped:** 2026-04-27

---

## Top-Level

```
YoloFarm/
├── backend/
│   ├── api/                  ← Spring Boot REST API
│   └── plantUML/             ← Architecture diagrams (PlantUML source)
├── frontend/                 ← React SPA (Vite + TypeScript)
├── simulator/
│   └── digital-twin/         ← Python digital twin simulator
├── scripts/                  ← PowerShell dev runner scripts
├── .agent/                   ← GSD agent skills & planning workflows
├── .github/                  ← GitHub Actions CI config (if any)
├── KNOWLEDGE_BASE.md         ← Project-wide documentation/wiki
└── README.md
```

---

## Backend — `backend/api/`

```
backend/api/
├── pom.xml                           ← Maven project descriptor
├── mvnw / mvnw.cmd                   ← Maven wrapper
├── openAPI.yaml                      ← Full OpenAPI 3 specification (~52KB)
├── .env / .env.example               ← Environment variables
├── src/
│   ├── main/
│   │   ├── java/com/yoloFarm/api/
│   │   │   ├── controller/           ← REST endpoints
│   │   │   │   ├── AuthController.java
│   │   │   │   ├── FarmController.java
│   │   │   │   ├── DeviceController.java
│   │   │   │   ├── RuleController.java
│   │   │   │   └── UserController.java
│   │   │   ├── dto/
│   │   │   │   ├── SensorData.java   ← Java record (immutable payload)
│   │   │   │   ├── request/          ← Input DTOs (validated)
│   │   │   │   └── response/         ← Output DTOs (JSON-serialized)
│   │   │   ├── entity/               ← JPA entities
│   │   │   │   ├── User.java
│   │   │   │   ├── Farm.java
│   │   │   │   ├── Device.java
│   │   │   │   ├── DeviceModel.java
│   │   │   │   ├── Rule.java
│   │   │   │   ├── TelemetryData.java
│   │   │   │   └── Notification.java
│   │   │   ├── enums/                ← Domain enumerations
│   │   │   │   ├── RoleEnum.java
│   │   │   │   ├── DeviceTypeEnum.java
│   │   │   │   ├── DeviceStatusEnum.java
│   │   │   │   ├── ConnectionStatusEnum.java
│   │   │   │   ├── OperatingModeEnum.java
│   │   │   │   ├── MetricTypeEnum.java
│   │   │   │   ├── RuleTypeEnum.java
│   │   │   │   └── ActionCommandEnum.java
│   │   │   ├── exception/
│   │   │   │   ├── GlobalExceptionHandler.java  ← @RestControllerAdvice
│   │   │   │   └── ConflictException.java
│   │   │   ├── repository/           ← Spring Data JPA repositories
│   │   │   │   ├── projection/       ← Interface projections for admin views
│   │   │   │   └── *.java
│   │   │   ├── security/             ← Spring Security components
│   │   │   │   ├── JwtAuthenticationFilter.java
│   │   │   │   ├── WebSocketAuthChannelInterceptor.java
│   │   │   │   ├── RestAuthenticationEntryPoint.java
│   │   │   │   └── RestAccessDeniedHandler.java
│   │   │   └── service/
│   │   │       ├── *.java            ← Core service interfaces + implementations
│   │   │       ├── impl/             ← Service implementations
│   │   │       │   ├── AuthServiceImpl.java
│   │   │       │   └── MqttSenderServiceImpl.java
│   │   │       ├── mqtt/             ← MQTT subsystem
│   │   │       │   ├── MqttReceiverService.java   ← Subject (Observer pattern)
│   │   │       │   ├── MqttSenderService.java     ← Interface
│   │   │       │   └── observer/
│   │   │       │       ├── Observer.java           ← Interface
│   │   │       │       ├── Subject.java            ← Interface
│   │   │       │       ├── DatabaseLoggerObserver.java
│   │   │       │       ├── RuleEngineObserver.java
│   │   │       │       └── WebSocketNotifierObserver.java
│   │   │       ├── strategy/         ← Irrigation strategy pattern
│   │   │       │   ├── IrrigationStrategy.java    ← Interface
│   │   │       │   ├── IrrigationContext.java     ← Context/router
│   │   │       │   ├── ManualStrategy.java
│   │   │       │   ├── AutoThresholdStrategy.java
│   │   │       │   └── ScheduledStrategy.java
│   │   │       └── automation/       ← Automation safety & state
│   │   │           ├── AutoIrrigationSafetyService.java
│   │   │           └── AutomationRuntimeStateService.java
│   │   └── resources/
│   │       ├── application.yml       ← Main Spring Boot config
│   │       ├── application.properties← Secondary config (may be empty)
│   │       ├── static/               ← Static resources
│   │       └── templates/            ← Thymeleaf templates (if any)
│   └── test/
│       └── java/com/yoloFarm/api/   ← Unit & integration tests
│           ├── ApiContractSerializationTest.java
│           ├── ApiEndpointContractTest.java
│           ├── AutoIrrigationSafetyServiceTest.java
│           ├── DeviceServiceRemovalApprovalTest.java
│           ├── DeviceServiceRenameSyncTest.java
│           ├── EntityMappingIntegrationTest.java
│           ├── FarmCrudIntegrationTest.java
│           ├── MqttReceiverServiceTest.java
│           ├── NotificationServiceRealtimePushTest.java
│           ├── RuleEngineObserverTest.java
│           ├── RuleServiceConditionPairingTest.java
│           ├── RuleServiceDeleteLifecycleTest.java
│           ├── RuleServiceSchedulePairingTest.java
│           └── TelemetryObserversTest.java
├── sql/                              ← SQL migration/seed scripts
└── scripts/                         ← Backend-specific scripts
```

---

## Frontend — `frontend/`

```
frontend/
├── index.html                        ← Vite entry HTML
├── vite.config.ts                    ← Vite configuration
├── tailwind.config.ts                ← Tailwind CSS configuration
├── postcss.config.js                 ← PostCSS (autoprefixer)
├── tsconfig.json                     ← TypeScript config
├── package.json
├── .env / .env.example
└── src/
    ├── main.tsx                      ← React entry point
    ├── App.tsx                       ← Router, global Toaster
    ├── index.css                     ← Global CSS / Tailwind directives
    ├── vite-env.d.ts                 ← Vite type declarations
    ├── stores/
    │   ├── authStore.ts              ← Zustand auth state (JWT, profile)
    │   └── notificationStore.ts     ← Zustand WS connection + notifications
    ├── types/                        ← TypeScript type definitions
    ├── lib/                          ← Utility functions (axios config, etc.)
    ├── pages/
    │   ├── auth/
    │   │   ├── LoginPage.tsx
    │   │   └── RegisterPage.tsx
    │   ├── farmer/
    │   │   ├── FarmsPage.tsx         ← Farm list for logged-in farmer
    │   │   └── FarmDetailPage.tsx    ← Main dashboard: devices + telemetry + rules
    │   ├── admin/
    │   │   ├── AdminDashboardPage.tsx
    │   │   ├── DeviceRequestsPage.tsx
    │   │   └── DeviceModelsPage.tsx
    │   └── ProfilePage.tsx
    └── components/
        ├── guards/
        │   ├── PublicRoute.tsx       ← Redirects authenticated users
        │   └── ProtectedRoute.tsx    ← JWT + role guard
        ├── layout/
        │   └── MainLayout.tsx        ← Sidebar/navbar shell
        ├── farms/                    ← Farm card, list components
        ├── devices/                  ← Device card, status badge
        ├── telemetry/                ← Recharts telemetry chart
        ├── rules/                    ← Rule form, rule list
        ├── admin/                    ← Admin-specific components
        └── ui/                       ← Generic primitives (buttons, modals)
```

---

## Simulator — `simulator/digital-twin/`

```
simulator/digital-twin/
├── main.py                           ← Single-file simulator (DigitalTwinManager)
├── profiles.json                     ← Device simulation profiles
├── profiles.example.json             ← Profile configuration reference
├── requirements.txt                  ← Python dependencies
├── .env / .env.example               ← Environment variables
├── .venv/                            ← Python virtual environment
├── sql/                              ← SQL scripts (device seeding etc.)
├── scripts/                          ← Simulator-specific scripts
└── tools/                            ← Utility tools
```

---

## Key File Locations

| Purpose | File |
|---|---|
| Spring Boot entry | (inferred: `ApiApplication.java` in root package) |
| OpenAPI spec | `backend/api/openAPI.yaml` |
| Application config | `backend/api/src/main/resources/application.yml` |
| MQTT receiver (core) | `backend/api/src/main/java/com/yoloFarm/api/service/mqtt/MqttReceiverService.java` |
| Rule engine | `backend/api/src/main/java/com/yoloFarm/api/service/mqtt/observer/RuleEngineObserver.java` |
| Safety watchdog | `backend/api/src/main/java/com/yoloFarm/api/service/automation/AutoIrrigationSafetyService.java` |
| Heartbeat janitor | `backend/api/src/main/java/com/yoloFarm/api/service/DeviceHeartbeatService.java` |
| Frontend router | `frontend/src/App.tsx` |
| Auth state | `frontend/src/stores/authStore.ts` |
| Simulator main | `simulator/digital-twin/main.py` |
| Architecture diagrams | `backend/plantUML/architecture_system.puml` |

---

## Naming Conventions

| Item | Convention |
|---|---|
| Java packages | `com.yoloFarm.api.<layer>` |
| Java classes | `PascalCase` + layer suffix (`Service`, `Controller`, `Repository`) |
| Java enums | `PascalCase` + `Enum` suffix |
| React components | `PascalCase` + `Page` / `Component` suffix |
| TypeScript stores | `camelCase` + `Store` suffix |
| REST JSON fields | `snake_case` (Jackson `SNAKE_CASE` strategy) |
| MQTT topics | `{username}/feeds/{feed_key}` |
| DB tables | `snake_case` plural (e.g. `devices`, `telemetry_data`) |
| Git branches | Feature branches off `main` |
