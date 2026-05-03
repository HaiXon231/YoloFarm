# TESTING.md — Test Structure & Practices (Backend)

**Last mapped:** 2026-04-28

---

## Scope
- `backend/api/` only

---

## Framework & Dependencies
- **JUnit 5** (via `spring-boot-starter-test`)
- **Mockito** (via `spring-boot-starter-test` + `@MockitoBean`)
- **Spring Boot Test** (`@SpringBootTest` for integration, `@WebMvcTest` for slice tests)
- **H2** (runtime scope) used for integration tests
- **Test dependencies in pom.xml:**
  - `spring-boot-starter-test` (test)
  - `spring-boot-starter-data-jpa-test` (test)
  - `spring-boot-starter-security-test` (test)
  - `spring-boot-starter-validation-test` (test)
  - `spring-boot-starter-webmvc-test` (test)
  - `com.h2database:h2` (runtime)

---

## Test Location
- Root: `backend/api/src/test/java/com/yoloFarm/api/`
- Subpackages: `service/automation/`, `service/mqtt/observer/`

---

## Test Classes

| Test Class | Type | Focus |
|---|---|---|
| `ApiContractSerializationTest` | `@SpringBootTest` | JSON SNAKE_CASE serialization contract |
| `ApiEndpointContractTest` | Integration | HTTP endpoint response contracts |
| `AutoIrrigationSafetyServiceTest` | Unit | Safety watchdog auto-off logic |
| `DeviceServiceRemovalApprovalTest` | Unit/Integration | Device removal + approval lifecycle |
| `DeviceServiceRenameSyncTest` | Unit | Device rename propagates to Adafruit API |
| `EntityMappingIntegrationTest` | Integration | JPA entity relationships, DB round-trip |
| `FarmCrudIntegrationTest` | Integration | Farm CRUD operations with H2 |
| `MqttReceiverServiceTest` | Unit | MQTT message parsing, feed key resolution, cache behavior |
| `NotificationServiceRealtimePushTest` | Unit | WebSocket notification push verification |
| `RuleEngineObserverTest` | Unit | Rule condition evaluation and cooldown |
| `RuleServiceConditionPairingTest` | Unit | Rule creation with sensor↔actuator pairing |
| `RuleServiceDeleteLifecycleTest` | Unit | Rule deletion cleans up associations |
| `RuleServiceSchedulePairingTest` | Unit | CRON rule pairing and scheduling |
| `TelemetryObserversTest` | Unit | Observer chain invocation |

---

## Testing Patterns

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
- `@MockitoBean` for Spring context mocks
- `Clock` injection for deterministic time-based tests

**H2 Integration:**
- H2 used via Spring Boot test auto-configuration

---

## What Is Tested
- JSON serialization SNAKE_CASE contract
- Rule engine condition evaluation logic
- Automation cooldown and safety watchdog
- Device lifecycle (register → approve → rename → remove)
- MQTT feed key cache (hit/miss/eviction)
- Observer notification chain
- Farm CRUD with ownership scoping
- Scheduled rule triggering

---

## What Is NOT Tested (Backend Gaps)
- AI analysis service (still a stub)
- WebSocket integration end-to-end
- Adafruit REST API client (would require network mock)
- JWT filter integration coverage is limited

---

## Running Tests

```powershell
# From backend/api/
./mvnw test

# Run specific test class
./mvnw test -Dtest=RuleEngineObserverTest

# Skip tests during build
./mvnw package -DskipTests
```

---

## Test Infrastructure Notes

1. **MQTT client must be mocked** in `@SpringBootTest` to avoid real Adafruit IO connections
2. **Clock bean** is injectable for deterministic time tests
3. **H2 vs PostgreSQL** differences can hide schema issues (use caution)
