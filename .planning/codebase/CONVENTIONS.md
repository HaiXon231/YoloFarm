# Coding Conventions

**Analysis Date:** 2026-05-18

## Naming Patterns

**Files:**
- Backend Java uses one public class/interface/enum per file with PascalCase names matching the type: `backend/api/src/main/java/com/yoloFarm/api/service/FarmService.java`, `backend/api/src/main/java/com/yoloFarm/api/controller/FarmController.java`, `backend/api/src/main/java/com/yoloFarm/api/enums/DeviceStatusEnum.java`.
- Backend tests use descriptive PascalCase class names ending in `Test`: `backend/api/src/test/java/com/yoloFarm/api/RuleServiceConditionPairingTest.java`, `backend/api/src/test/java/com/yoloFarm/api/ApiEndpointContractTest.java`.
- Frontend React components and pages use PascalCase `.tsx` filenames: `frontend/src/components/devices/ActuatorCard.tsx`, `frontend/src/pages/farmer/FarmDetailPage.tsx`.
- Frontend state and library modules use camelCase `.ts` filenames: `frontend/src/stores/authStore.ts`, `frontend/src/lib/websocket.ts`, `frontend/src/lib/axios.ts`.
- Frontend tests are colocated in `__tests__` directories and use `.test.ts` or `.test.tsx`: `frontend/src/stores/__tests__/authStore.test.ts`, `frontend/src/pages/__tests__/FarmDetailPage.test.tsx`.
- Simulator Python modules use snake_case filenames: `simulator/digital-twin/main.py`, `simulator/digital-twin/tools/feed_key_manager.py`.

**Functions:**
- Java service/controller methods use camelCase and describe the action and domain object: `createFarm`, `getFarmsByUserId`, `deleteFarm` in `backend/api/src/main/java/com/yoloFarm/api/service/FarmService.java`.
- Java test methods use behavior-style camelCase names with `should...when...` where practical: `shouldCreateInactiveConditionRule_whenOppositeRuleIsMissing` in `backend/api/src/test/java/com/yoloFarm/api/RuleServiceConditionPairingTest.java`.
- React components are default-exported PascalCase functions: `export default function Modal(...)` in `frontend/src/components/ui/Modal.tsx`.
- Frontend handlers and helpers use camelCase with action prefixes: `handleEsc` in `frontend/src/components/ui/Modal.tsx`, `getApiErrorMessage` in `frontend/src/lib/axios.ts`.
- Python functions and methods use snake_case: `fetch_devices`, `allocate_unique_key`, `_fetch_active_devices` in `simulator/digital-twin/tools/feed_key_manager.py` and `simulator/digital-twin/main.py`.

**Variables:**
- Java locals and parameters use camelCase: `ownerId`, `farmId`, `currentUser` in `backend/api/src/main/java/com/yoloFarm/api/controller/FarmController.java`.
- Java enum constants use uppercase snake case: `FARMER`, `ADMIN`, `SENSOR`, `ACTUATOR` in `backend/api/src/main/java/com/yoloFarm/api/enums/RoleEnum.java` and `backend/api/src/main/java/com/yoloFarm/api/enums/DeviceTypeEnum.java`.
- Frontend variables use camelCase, while API payload fields follow backend JSON `snake_case`: `accessToken` and `isAuthenticated` in `frontend/src/stores/authStore.ts`; `unread_count` and `created_at` in `frontend/src/stores/__tests__/notificationStore.test.ts`.
- Python variables use snake_case: `feed_key`, `owner_id`, `dry_run` in `simulator/digital-twin/tools/feed_key_manager.py`.

**Types:**
- Java classes use PascalCase with layer suffixes: `FarmController`, `FarmService`, `FarmRepository`, `FarmResponse` in `backend/api/src/main/java/com/yoloFarm/api/`.
- Java interfaces use PascalCase and no `I` prefix: `AdafruitApiService`, `IrrigationStrategy`, `Observer`, `Subject` in `backend/api/src/main/java/com/yoloFarm/api/service/`.
- Java implementations use `Impl` suffix when an explicit interface exists: `backend/api/src/main/java/com/yoloFarm/api/service/impl/AuthServiceImpl.java`, `backend/api/src/main/java/com/yoloFarm/api/service/impl/MqttSenderServiceImpl.java`.
- Java enums use an `Enum` suffix: `RoleEnum`, `RuleTypeEnum`, `OperatingModeEnum` in `backend/api/src/main/java/com/yoloFarm/api/enums/`.
- TypeScript interfaces use PascalCase and domain suffixes such as `Request`, `Response`, and `Props`: `LoginRequest`, `DeviceResponse`, `ModalProps` in `frontend/src/types/index.ts` and `frontend/src/components/ui/Modal.tsx`.
- TypeScript literal unions model backend enums at the boundary: `DeviceType`, `MetricType`, `RuleType`, `ActionCommand` in `frontend/src/types/index.ts`.
- Python dataclasses use PascalCase: `DeviceInfo`, `DeviceRuntime`, `DeviceRow` in `simulator/digital-twin/main.py` and `simulator/digital-twin/tools/feed_key_manager.py`.

## Code Style

**Formatting:**
- Frontend uses TypeScript strict mode via `frontend/tsconfig.json`; no Prettier config is detected. Follow the existing source style: two-space indentation, semicolon-free production `.ts/.tsx`, single quotes, trailing commas in multiline object/function calls where already present.
- Frontend tests use the same imports and assertions but currently include semicolons: `frontend/src/stores/__tests__/authStore.test.ts`. Prefer matching the file being edited rather than normalizing unrelated style.
- Backend has no Checkstyle or Spotless configuration in `backend/api/pom.xml`. Use Java 21 conventions with four-space indentation, annotations above declarations, and constructor injection through Lombok `@RequiredArgsConstructor`.
- Backend XML in `backend/api/pom.xml` uses tabs in existing sections. Preserve local indentation when editing.
- Python has no `pyproject.toml`, `ruff.toml`, or formatter config detected. Follow PEP 8-style four-space indentation and type hints as shown in `simulator/digital-twin/tools/feed_key_manager.py`.

**Linting:**
- Frontend lint tooling is not configured in `frontend/package.json`; no `eslint.config.*`, `.eslintrc*`, `.prettierrc*`, or `biome.json` is detected.
- Backend lint tooling is not configured in `backend/api/pom.xml`.
- Treat TypeScript compile (`npm run build` in `frontend/package.json`) and Maven test compile (`.\mvnw.cmd test` in `backend/api/`) as the enforced quality gates.

## Import Organization

**Order:**
1. Java: package declaration, project imports (`com.yoloFarm.api...`), framework/third-party imports (`org.springframework...`, `jakarta...`, `lombok...`), JDK imports, then static imports. See `backend/api/src/test/java/com/yoloFarm/api/RuleServiceConditionPairingTest.java`.
2. Frontend: React/vendor imports, local relative imports, aliased project imports, then type-only imports when applicable. Existing files vary, so keep imports readable and remove unused imports during edits. See `frontend/src/pages/__tests__/FarmDetailPage.test.tsx` and `frontend/src/lib/axios.ts`.
3. Python: standard library imports, third-party imports, then local declarations. See `simulator/digital-twin/main.py`.

**Path Aliases:**
- Frontend uses `@/*` for `frontend/src/*` in `frontend/tsconfig.json` and `frontend/vite.config.ts`.
- Use `@/types`, `@/lib/axios`, and `@/lib/websocket` for cross-directory frontend imports. Use relative imports for same-directory or parent test targets, such as `../authStore` in `frontend/src/stores/__tests__/authStore.test.ts`.
- Backend uses Java package imports under `com.yoloFarm.api`; no module aliasing is used.

## Error Handling

**Patterns:**
- Backend services throw typed exceptions (`EntityNotFoundException`, `ConflictException`, `AccessDeniedException`, `IllegalStateException`, `IllegalArgumentException`) and let `backend/api/src/main/java/com/yoloFarm/api/exception/GlobalExceptionHandler.java` convert them to `ErrorResponse`.
- Backend controllers should stay thin: validate request shape with `@Valid`, `@RequestBody`, `@PathVariable`, and `@AuthenticationPrincipal`, then delegate to services. See `backend/api/src/main/java/com/yoloFarm/api/controller/FarmController.java`.
- Backend error responses use `{code, message, details}` in `backend/api/src/main/java/com/yoloFarm/api/dto/response/ErrorResponse.java`; keep new handlers aligned with `GlobalExceptionHandler`.
- Frontend API calls should catch errors at page/component/store boundaries and use `getApiErrorMessage` from `frontend/src/lib/axios.ts` when showing user-facing failures.
- `frontend/src/lib/axios.ts` owns global 401 handling: clear `access_token` and `role`, then redirect to `/login` unless already there.
- Frontend store methods use `try/catch` and mutate Zustand state directly through `set`, as in `frontend/src/stores/authStore.ts` and `frontend/src/stores/notificationStore.ts`.
- Simulator scripts catch HTTP and JSON errors locally and print actionable status messages, as in `simulator/digital-twin/tools/e2e_create_device_and_verify.py`.

## Logging

**Framework:** SLF4J/Lombok for backend, `console` for frontend, Python `logging` for simulator.

**Patterns:**
- Backend classes that log should use Lombok `@Slf4j`, then `log.info`, `log.warn`, `log.debug`, or `log.error` with `{}` placeholders. Examples: `backend/api/src/main/java/com/yoloFarm/api/service/RuleService.java`, `backend/api/src/main/java/com/yoloFarm/api/service/mqtt/MqttReceiverService.java`.
- Backend global unexpected exceptions are logged once in `backend/api/src/main/java/com/yoloFarm/api/exception/GlobalExceptionHandler.java`; avoid duplicate logs for expected validation/domain failures.
- Frontend logging is mostly limited to transport/debug paths in `frontend/src/lib/websocket.ts` and failure paths in `frontend/src/stores/notificationStore.ts`. Do not add noisy logs to ordinary render paths.
- Simulator logs through `self.logger = logging.getLogger("digital_twin")` in `simulator/digital-twin/main.py`; use structured format strings with `%s`/`%.2f`.

## Comments

**When to Comment:**
- Add comments for non-obvious behavior, external constraints, browser/jsdom workarounds, or safety conditions.
- Keep comments close to the code they explain. Examples include the ResizeObserver jsdom mock in `frontend/src/setupTests.ts`, the Recharts/WebSocket mocks in `frontend/src/pages/__tests__/FarmDetailPage.test.tsx`, and threshold profile overrides in `simulator/digital-twin/main.py`.
- Avoid comments that restate simple assignments or obvious framework annotations.

**JSDoc/TSDoc:**
- Not detected as a regular convention. Frontend code relies on TypeScript interfaces and type aliases in `frontend/src/types/index.ts` rather than TSDoc.
- Java code does not use Javadoc as a regular convention; method names and DTO types carry intent.

## Function Design

**Size:** Keep controllers thin and services focused. Complex domain decisions belong in services such as `backend/api/src/main/java/com/yoloFarm/api/service/RuleService.java`; HTTP wiring belongs in controllers such as `backend/api/src/main/java/com/yoloFarm/api/controller/RuleController.java`.

**Parameters:** Prefer typed DTOs for request bodies (`FarmCreateRequest`, `RuleCreateRequest`, `DeviceCommandRequest`) and `UUID` for identifiers in backend APIs. Frontend components accept a single props interface when props exceed trivial cases, as in `frontend/src/components/ui/Modal.tsx`.

**Return Values:** Backend controllers return `ResponseEntity<?>` for HTTP responses, services return DTOs or void for commands, and repositories return entities/projections/`Optional`. Frontend helpers return typed values or `Promise<void>` for state actions. Python helpers return concrete typed collections where practical, such as `list[DeviceRow]` and `set[str]` in `simulator/digital-twin/tools/feed_key_manager.py`.

## Module Design

**Exports:** Frontend components use default exports for page/component modules and named exports for reusable stores/helpers/types: `useAuthStore` in `frontend/src/stores/authStore.ts`, `getApiErrorMessage` in `frontend/src/lib/axios.ts`, and interfaces in `frontend/src/types/index.ts`.

**Barrel Files:** `frontend/src/types/index.ts` is the primary barrel for frontend DTO and domain types. No broad component barrel files are detected; import components by direct path.

---

*Convention analysis: 2026-05-18*
