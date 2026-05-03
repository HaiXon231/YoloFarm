# STRUCTURE.md — Directory Layout (Backend)

**Last mapped:** 2026-04-28

---

## Scope
- `backend/api/` only

---

## Backend — `backend/api/`

```
backend/api/
├── pom.xml                           ← Maven project descriptor
├── mvnw / mvnw.cmd                   ← Maven wrapper
├── openAPI.yaml                      ← OpenAPI 3 specification
├── .env / .env.example               ← Environment variables
├── scripts/
│   └── run-local.ps1                 ← Local dev runner
├── src/
│   ├── main/
│   │   ├── java/com/yoloFarm/api/
│   │   │   ├── ApiApplication.java   ← Spring Boot entrypoint
│   │   │   ├── config/               ← Security, CORS, WS, MQTT configs
│   │   │   ├── controller/           ← REST endpoints
│   │   │   ├── dto/                  ← Request/response DTOs
│   │   │   ├── entity/               ← JPA entities
│   │   │   ├── enums/                ← Domain enumerations
│   │   │   ├── exception/            ← Global exception handling
│   │   │   ├── repository/           ← Spring Data JPA repositories
│   │   │   ├── security/             ← JWT + WS auth filters
│   │   │   └── service/              ← Core services + submodules
│   │   │       ├── automation/       ← Automation safety & runtime state
│   │   │       ├── impl/             ← Service implementations
│   │   │       ├── mqtt/             ← MQTT subsystem + observers
│   │   │       ├── security/         ← JWT service
│   │   │       └── strategy/         ← Irrigation strategy pattern
│   │   └── resources/
│   │       ├── application.yml       ← Main Spring Boot config
│   │       ├── application.properties← Secondary config (may be empty)
│   │       ├── db/migration/          ← Flyway migrations
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
│           ├── TelemetryObserversTest.java
│           └── service/
│               ├── automation/AutoIrrigationSafetyServiceTest.java
│               └── mqtt/observer/RuleEngineObserverTest.java
```

---

## Key File Locations

| Purpose | File |
|---|---|
| Spring Boot entry | `backend/api/src/main/java/com/yoloFarm/api/ApiApplication.java` |
| OpenAPI spec | `backend/api/openAPI.yaml` |
| Application config | `backend/api/src/main/resources/application.yml` |
| MQTT receiver (core) | `backend/api/src/main/java/com/yoloFarm/api/service/mqtt/MqttReceiverService.java` |
| Rule engine | `backend/api/src/main/java/com/yoloFarm/api/service/mqtt/observer/RuleEngineObserver.java` |
| Safety watchdog | `backend/api/src/main/java/com/yoloFarm/api/service/automation/AutoIrrigationSafetyService.java` |
| Heartbeat janitor | `backend/api/src/main/java/com/yoloFarm/api/service/DeviceHeartbeatService.java` |
| CORS config | `backend/api/src/main/java/com/yoloFarm/api/config/CorsConfig.java` |
| WebSocket config | `backend/api/src/main/java/com/yoloFarm/api/config/WebSocketConfig.java` |
| Security config | `backend/api/src/main/java/com/yoloFarm/api/config/SecurityConfig.java` |

---

## Naming Conventions

| Item | Convention |
|---|---|
| Java packages | `com.yoloFarm.api.<layer>` |
| Java classes | `PascalCase` + layer suffix (`Service`, `Controller`, `Repository`) |
| Java enums | `PascalCase` + `Enum` suffix |
| REST JSON fields | `snake_case` (Jackson `SNAKE_CASE` strategy) |
| MQTT topics | `{username}/feeds/{feed_key}` |
| DB tables | `snake_case` plural (e.g. `devices`, `telemetry_data`) |
