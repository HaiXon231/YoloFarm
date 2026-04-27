# CONVENTIONS.md — Code Style & Patterns

**Last mapped:** 2026-04-27

---

## Backend (Java / Spring Boot)

### General Style
- **Java version:** Java 25 (preview/LTS)
- **Build tool:** Maven with wrapper (`mvnw`)
- **Base package:** `com.yoloFarm.api`
- **Annotation-driven:** All beans declared via `@Service`, `@Component`, `@Repository`, `@RestController`
- **Constructor injection:** All dependencies injected via `@RequiredArgsConstructor` (Lombok-generated constructor) — no field injection (`@Autowired`)
- **Immutability preference:** Sensor data modeled as Java record (`SensorData.java`)

### Naming Conventions
| Item | Convention | Example |
|---|---|---|
| Classes | PascalCase + layer suffix | `FarmService`, `DeviceController` |
| Interfaces | PascalCase (no prefix) | `IrrigationStrategy`, `Observer`, `Subject` |
| Implementations | Suffix `Impl` | `AuthServiceImpl`, `MqttSenderServiceImpl` |
| Enums | PascalCase + `Enum` suffix | `DeviceStatusEnum`, `RuleTypeEnum` |
| Constants | `UPPER_SNAKE_CASE` | `OBSERVER_CORE_THREADS`, `OBSERVER_MAX_THREADS` |
| Variables/params | `camelCase` | `feedKey`, `mqttClient` |
| DB columns | `snake_case` via `@Column(name="...")` | `adafruit_feed_key`, `connection_status` |

### JSON Serialization
- **Strategy:** Jackson globally configured to `SNAKE_CASE` (via `application.yml`)
  ```yaml
  spring.jackson.property-naming-strategy: SNAKE_CASE
  ```
- All response DTOs use `camelCase` Java field names; Jackson auto-converts to `snake_case` JSON
- Verified by `ApiContractSerializationTest.java`

### Entity Conventions
- All entities use `@GeneratedValue(strategy = GenerationType.UUID)` for primary keys
- All UUIDs are `java.util.UUID` type
- Lazy loading: `@ManyToOne(fetch = FetchType.LAZY)` is the default
- Timestamps: `java.time.LocalDateTime` or `java.time.Instant`
- Enums stored as strings: `@Enumerated(EnumType.STRING)`
- Lombok used on all entities: `@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder`

### Error Handling
- **Single global handler:** `GlobalExceptionHandler.java` annotated `@RestControllerAdvice`
- All exceptions mapped to `ErrorResponse` DTO with `{code, message, details}`
- Known mapped exceptions:
  - `EntityNotFoundException` → 404
  - `ConflictException` → 409
  - `MethodArgumentNotValidException` → 400
  - `ConstraintViolationException` → 400
  - `BadCredentialsException` → 401
  - `AuthenticationException` → 401
  - `AccessDeniedException` → 403
  - `IllegalStateException` → 400
  - `IllegalArgumentException` → 400
  - `HttpMessageNotReadableException` → 400
  - `DataIntegrityViolationException` → 409
  - `Exception` (catch-all) → 500 (logs with `@Slf4j`)
- **No `try-catch` in controllers** — all bubbled to global handler

### Logging
- `@Slf4j` (Lombok) on all services and components
- Vietnamese-language log messages (mixed Vietnamese/English codebase)
- Log levels: `log.info()` for normal operations, `log.warn()` for soft failures, `log.error()` for exceptions, `log.debug()` for verbose internal state
- Example: `log.info("MqttReceiver: Đã Subscribe thành công topic: [{}]", wildcardTopic)`

### Concurrency Patterns
- `MqttReceiverService` uses a bounded `ThreadPoolExecutor` for observer dispatch:
  - 4 core threads / 8 max / queue capacity 1000
  - `CallerRunsPolicy` fallback when queue full
- Device-feed cache: `ConcurrentHashMap<String, Device>` — O(1) lookup, thread-safe
- `AutomationRuntimeStateService` manages in-memory timestamp state for cooldown/safety logic
- `AiAnalysisService` uses `CopyOnWriteArrayList` for in-memory log list (read-heavy)
- Actuator state update bypasses Hibernate: uses `JdbcTemplate.update()` directly to avoid Detached Entity exceptions on MQTT callback threads

### Service Layer Patterns
- `@Transactional(readOnly = true)` on read-heavy services; `@Transactional` on write methods
- Farm ownership validation before any farm-scoped operation (throws `EntityNotFoundException`)
- MQTT feed cache warmed on device approval via `MqttReceiverService.cacheFeedKey()`
- Feed cache evicted on device removal via `MqttReceiverService.evictFeedKeyCache()`

---

## Frontend (TypeScript / React)

### General Style
- **Framework:** React 19 functional components only
- **Language:** TypeScript with strict mode (`tsconfig.json`)
- **Module system:** ESM (`"type": "module"`)
- **Formatting:** Not explicitly configured (no ESLint/Prettier config observed)

### Component Conventions
- All components are `.tsx` files with `PascalCase` names
- Export pattern: `export default function ComponentName()`
- Route-level components suffixed `Page`: `LoginPage`, `FarmDetailPage`
- Shared components organized by domain in `src/components/{domain}/`

### State Management (Zustand)
- Stores are files in `src/stores/` suffixed `Store`
- `authStore.ts`: persists JWT to `localStorage` via `loadFromStorage()`; fetches profile on auth
- `notificationStore.ts`: manages WebSocket lifecycle (STOMP connection), notification list, unread count
- No Redux, no React Context for global state — Zustand only

### API Calls
- HTTP client: `axios` — likely configured in `src/lib/` with base URL and auth header injection
- All REST calls go through the configured axios instance (token from authStore)

### Routing Conventions
- Role guard via `requiredRole` prop on `ProtectedRoute`:
  ```tsx
  <ProtectedRoute requiredRole="FARMER"><FarmsPage /></ProtectedRoute>
  ```
- Unauthenticated users redirected to `/login`
- Unknown routes caught by `<Route path="*">` → redirect to `/login`

### WebSocket / Real-time
- STOMP client from `@stomp/stompjs`, SockJS transport
- Subscriptions managed inside `notificationStore`
- Charts updated reactively via Zustand store subscriptions + Recharts

### Styling
- **Tailwind CSS 3.4** — utility-first classes
- Custom font: `Manrope` (referenced in toast config)
- Dark mode: partially implemented via Tailwind
- Toast styling: custom `react-hot-toast` with branded colors (`#006947` green, `#b31b25` red)

---

## Simulator (Python)

### General Style
- **Single-file architecture:** All logic in `main.py` — no module splitting
- **Dataclasses:** `DeviceInfo` and `DeviceRuntime` use `@dataclass`
- **Threading:** One daemon thread per device runtime
- **RLock:** `threading.RLock()` for all shared state (`_runtimes` dict)
- **Logging:** Python `logging` module with format `%(asctime)s %(levelname)s [%(name)s] %(message)s`

### Configuration Pattern
- `python-dotenv` loaded at startup; missing required vars cause `RuntimeError` at launch
- Profile JSON layered: `defaults` < `metrics` < `devices` < `feed:{key}` (most specific wins)
- Simulation patterns: `random_walk` (default), `constant`, `sine`, `actuator_state`

### Signal Handling
- `SIGINT` and `SIGTERM` both map to `manager.stop()` for graceful shutdown

---

## Cross-Cutting Conventions

### Environment Variables
- All secrets in `.env` (git-ignored), template in `.env.example`
- No hardcoded credentials in source code
- Backend reads via `${ENV_VAR}` in `application.yml`; Python reads via `os.getenv()`

### PlantUML Diagrams
- Architecture diagrams maintained in `backend/plantUML/` as `.puml` files
- `architecture_system.puml` is the primary system diagram (~72 lines)

### OpenAPI Spec
- Full API spec maintained at `backend/api/openAPI.yaml` (~52KB)
- Used for frontend API contract reference and potential code generation
