# CONVENTIONS.md — Code Style & Patterns (Backend)

**Last mapped:** 2026-04-28

---

## Scope
- `backend/api/` only

---

## General Style
- **Java version:** Java 21
- **Build tool:** Maven with wrapper (`mvnw`)
- **Base package:** `com.yoloFarm.api`
- **Annotation-driven:** Beans declared via `@Service`, `@Component`, `@Repository`, `@RestController`
- **Constructor injection:** `@RequiredArgsConstructor` (Lombok) — no field injection
- **Immutability preference:** Sensor data modeled as Java record (`SensorData.java`)

---

## Naming Conventions
| Item | Convention | Example |
|---|---|---|
| Classes | PascalCase + layer suffix | `FarmService`, `DeviceController` |
| Interfaces | PascalCase (no prefix) | `IrrigationStrategy`, `Observer`, `Subject` |
| Implementations | Suffix `Impl` | `AuthServiceImpl`, `MqttSenderServiceImpl` |
| Enums | PascalCase + `Enum` suffix | `DeviceStatusEnum`, `RuleTypeEnum` |
| Constants | `UPPER_SNAKE_CASE` | `OBSERVER_CORE_THREADS`, `OBSERVER_MAX_THREADS` |
| Variables/params | `camelCase` | `feedKey`, `mqttClient` |
| DB columns | `snake_case` via `@Column(name="...")` | `adafruit_feed_key`, `connection_status` |

---

## JSON Serialization
- **Strategy:** Jackson globally configured to `SNAKE_CASE` (via `application.yml`)
  ```yaml
  spring.jackson.property-naming-strategy: SNAKE_CASE
  ```
- DTO fields remain `camelCase` in Java; Jackson converts to `snake_case` JSON
- Verified by `ApiContractSerializationTest.java`

---

## Entity Conventions
- UUID primary keys: `@GeneratedValue(strategy = GenerationType.UUID)`
- Lazy loading defaults: `@ManyToOne(fetch = FetchType.LAZY)`
- Enums stored as strings: `@Enumerated(EnumType.STRING)`
- Lombok on entities: `@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder`

---

## Error Handling
- **Global handler:** `GlobalExceptionHandler.java` (`@RestControllerAdvice`)
- Errors mapped to `ErrorResponse` with `{code, message, details}`
- Controllers do not catch/handle exceptions directly

---

## Logging
- `@Slf4j` (Lombok) on services and components
- Mixed Vietnamese/English log messages (team convention)
- Standard levels: `info` for normal ops, `warn` for soft failures, `error` for exceptions

---

## Concurrency Patterns
- `MqttReceiverService` uses a bounded `ThreadPoolExecutor` for observer dispatch
  - 4 core / 8 max / queue capacity 1000
  - `CallerRunsPolicy` fallback when queue full
- Feed key cache: `ConcurrentHashMap<String, Device>`
- `AutomationRuntimeStateService` uses `ConcurrentHashMap` for cooldown and auto-on state
- `AiAnalysisService` stores logs in `CopyOnWriteArrayList` (in-memory)
- Device status updates from MQTT use `JdbcTemplate.update()` to avoid detached entity issues

---

## Service Layer Patterns
- `@Transactional(readOnly = true)` on read-heavy services; `@Transactional` on write methods
- Ownership validation before farm-scoped operations
- Feed cache warmed/evicted via `MqttReceiverService.cacheFeedKey()` / `evictFeedKeyCache()`

---

## Environment Variables
- Secrets in `.env` (git-ignored), template in `.env.example`
- Backend reads via `${ENV_VAR}` in `application.yml`
