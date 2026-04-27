# TESTING.md — Test Structure & Practices

**Last mapped:** 2026-04-27

---

## Backend Testing (Java / JUnit 5)

### Framework & Dependencies
- **JUnit 5** (via `spring-boot-starter-test`)
- **Mockito** (via `spring-boot-starter-test` + `MockitoBean`)
- **Spring Boot Test** (`@SpringBootTest` for integration, `@WebMvcTest` for slice tests)
- **H2 in-memory database** — used for integration tests that require DB
- **Test dependencies in pom.xml:**
  - `spring-boot-starter-test` (scope: test)
  - `spring-boot-starter-data-jpa-test` (scope: test)
  - `spring-boot-starter-security-test` (scope: test)
  - `spring-boot-starter-validation-test` (scope: test)
  - `spring-boot-starter-webmvc-test` (scope: test)
  - `com.h2database:h2` (scope: runtime — used by test context)

### Test Location
All tests in: `backend/api/src/test/java/com/yoloFarm/api/`

Flat structure — all test classes at the same package level (no subpackages).

### Test Classes

| Test Class | Type | Focus |
|---|---|---|
| `ApiContractSerializationTest` | `@SpringBootTest` | JSON SNAKE_CASE serialization contract for all DTOs |
| `ApiEndpointContractTest` | Integration | HTTP endpoint response contract validation |
| `AutoIrrigationSafetyServiceTest` | Unit | Safety watchdog auto-off logic, cooldown, clock injection |
| `DeviceServiceRemovalApprovalTest` | Unit/Integration | Device removal + approval lifecycle (feed key eviction) |
| `DeviceServiceRenameSyncTest` | Unit | Device rename propagates to Adafruit API |
| `EntityMappingIntegrationTest` | Integration | JPA entity relationships, DB round-trip |
| `FarmCrudIntegrationTest` | Integration | Farm CRUD operations with H2 |
| `MqttReceiverServiceTest` | Unit | MQTT message parsing, feed key resolution, cache behavior |
| `NotificationServiceRealtimePushTest` | Unit | WebSocket notification push verification |
| `RuleEngineObserverTest` | Unit | Rule condition evaluation, cooldown, actuator state skip |
| `RuleServiceConditionPairingTest` | Unit | Rule creation with sensor↔actuator pairing validation |
| `RuleServiceDeleteLifecycleTest` | Unit | Rule deletion cleans up associations |
| `RuleServiceSchedulePairingTest` | Unit | CRON rule pairing and scheduling |
| `TelemetryObserversTest` | Unit | Observer chain invocation (DatabaseLogger + WebSocket) |

### Testing Patterns

**Integration Tests (`@SpringBootTest`):**
```java
@SpringBootTest
class ApiContractSerializationTest {
    @MockitoBean
    private IMqttClient mqttClient;  // Mocked to prevent real MQTT connection

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void loginResponseShouldSerializeSnakeCase() throws Exception {
        // Build DTO → serialize → assert JSON keys
    }
}
```

**Unit Tests with Mockito:**
- `@MockitoBean` for Spring context mocks (replaces `@MockBean` in Spring 6.2+)
- Clock injection (`Clock clock`) for deterministic time-based tests
  - `AutoIrrigationSafetyService` and `RuleEngineObserver` accept `Clock` bean
  - Tests pass a fixed `Clock.fixed(...)` for reproducible behavior

**Observer Testing:**
```java
// Verify observer chain fires correctly
// Mock Subject, inject observers, call notifyObservers(sensorData)
// Assert mocked services were called with correct args
```

**H2 Integration:**
- H2 used transparently when `spring-boot-starter-data-jpa-test` activates test profile
- `ddl-auto: create-drop` for clean state per test class

### What Is Tested
- ✅ JSON serialization SNAKE_CASE contract (critical — frontend depends on this)
- ✅ Rule engine condition evaluation logic
- ✅ Automation cooldown and safety watchdog
- ✅ Device lifecycle (register → approve → rename → remove)
- ✅ MQTT feed key cache (hit/miss/eviction)
- ✅ Observer notification chain
- ✅ Farm CRUD with ownership scoping
- ✅ Scheduled rule triggering

### What Is NOT Tested (Gaps)
- ❌ Frontend (no frontend test suite detected)
- ❌ Simulator/digital twin (no Python tests detected)
- ❌ AI analysis service (stub — no real behavior to test)
- ❌ WebSocket integration end-to-end
- ❌ Adafruit REST API client (would require network mock)
- ❌ JWT filter (security integration test coverage unclear)

---

## Frontend Testing

**No test suite detected.** The `package.json` has no `test` script and no testing framework (Vitest, Jest, Playwright, Cypress) listed in dependencies or devDependencies.

> ⚠️ Frontend test coverage is zero. This is a significant gap for a production application.

---

## Simulator Testing

**No test suite detected.** The `simulator/digital-twin/` directory contains no `test_*.py` or `*_test.py` files and no testing framework in `requirements.txt` (no `pytest`, `unittest`).

> ⚠️ Simulator test coverage is zero.

---

## Running Tests

### Backend
```powershell
# From backend/api/
./mvnw test

# Run specific test class
./mvnw test -Dtest=RuleEngineObserverTest

# Skip tests during build
./mvnw package -DskipTests
```

### Frontend
```
# Not available — no test runner configured
npm run test  # ← This script does not exist
```

---

## Test Infrastructure Notes

1. **MQTT Client must be mocked** in `@SpringBootTest` tests — real `IMqttClient` would attempt to connect to Adafruit IO on startup
2. **Clock bean** is injectable — `AutoIrrigationSafetyService` and `RuleEngineObserver` accept `Clock` via constructor for deterministic time testing
3. **H2 dialect** compatibility: Spring Boot auto-detects H2 for test profile; entities use standard JPA annotations compatible with both H2 and PostgreSQL
4. **No separate test `application.yml`** — test configuration relies on Spring Boot test slices and H2 auto-configuration
