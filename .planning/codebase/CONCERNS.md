# Codebase Concerns

**Analysis Date:** 2026-05-18

## Tech Debt

**AI analysis is a mock implementation:**
- Issue: `backend/api/src/main/java/com/yoloFarm/api/service/AiAnalysisService.java:38` returns a mock label, `backend/api/src/main/java/com/yoloFarm/api/service/AiAnalysisService.java:40` points to mock storage, and `backend/api/src/main/java/com/yoloFarm/api/service/AiAnalysisService.java:25` stores logs in an in-memory list.
- Files: `backend/api/src/main/java/com/yoloFarm/api/service/AiAnalysisService.java`, `backend/api/src/main/java/com/yoloFarm/api/controller/FarmController.java`
- Impact: `/api/v1/farms/{farmId}/ai-analysis` appears production-ready but does not call an AI provider, persist uploaded files, or keep logs after restart.
- Fix approach: Add a durable `ai_analysis_logs` table, object storage or database-backed file metadata, an AI provider boundary, and tests around `FarmController.analyzeAi` and `AiAnalysisService.analyzeImage`.

**Frontend admin detail drawer uses untyped dynamic data:**
- Issue: `frontend/src/components/admin/AdminDetailDrawer.tsx:16` stores `any[]`, `frontend/src/components/admin/AdminDetailDrawer.tsx:35` calls a dynamic endpoint without a typed response, and `frontend/src/pages/admin/AdminDashboardPage.tsx:117` casts card types with `as any`.
- Files: `frontend/src/components/admin/AdminDetailDrawer.tsx`, `frontend/src/pages/admin/AdminDashboardPage.tsx`, `frontend/src/types/index.ts`
- Impact: Admin table rendering can drift from API response contracts without TypeScript catching missing columns, renamed fields, or unexpected values.
- Fix approach: Define discriminated admin drawer response types in `frontend/src/types/index.ts`, map each drawer type to a typed endpoint and renderer, and remove `any` casts from admin dashboard code.

**Hardware lifecycle mixes database transactions with remote Adafruit calls:**
- Issue: `backend/api/src/main/java/com/yoloFarm/api/service/DeviceService.java:211` starts a transactional approval flow, then `backend/api/src/main/java/com/yoloFarm/api/service/DeviceService.java:230` calls Adafruit before saving the device. Removal similarly calls `backend/api/src/main/java/com/yoloFarm/api/service/DeviceService.java:316` inside the cleanup path. `backend/api/src/main/java/com/yoloFarm/api/service/impl/AdafruitApiServiceImpl.java:33` creates a plain `RestTemplate`.
- Files: `backend/api/src/main/java/com/yoloFarm/api/service/DeviceService.java`, `backend/api/src/main/java/com/yoloFarm/api/service/impl/AdafruitApiServiceImpl.java`
- Impact: A remote timeout or partial Adafruit failure can hold a database transaction open, create cross-system inconsistency, and make retry behavior ambiguous.
- Fix approach: Move remote provisioning/deprovisioning behind an outbox or explicit saga-style state transition, configure HTTP connect/read timeouts on a bean-managed client, and make retries idempotent by feed key.

**Historical bug tags remain in production source:**
- Issue: Files such as `backend/api/src/main/java/com/yoloFarm/api/service/DeviceService.java:98`, `backend/api/src/main/java/com/yoloFarm/api/service/RuleService.java:148`, and `backend/api/src/main/java/com/yoloFarm/api/service/mqtt/MqttReceiverService.java:77` contain `BUG-*` comments describing fixed behavior.
- Files: `backend/api/src/main/java/com/yoloFarm/api/service/DeviceService.java`, `backend/api/src/main/java/com/yoloFarm/api/service/RuleService.java`, `backend/api/src/main/java/com/yoloFarm/api/service/mqtt/MqttReceiverService.java`, `backend/api/src/main/java/com/yoloFarm/api/service/strategy/ManualStrategy.java`, `backend/api/src/main/java/com/yoloFarm/api/service/strategy/AutoThresholdStrategy.java`, `backend/api/src/main/java/com/yoloFarm/api/service/strategy/ScheduledStrategy.java`
- Impact: Future maintainers cannot distinguish active defects from historical fix notes, which weakens TODO/BUG scanning as a signal.
- Fix approach: Convert fixed bug comments to behavior-oriented comments only where they clarify non-obvious invariants, and move historical references to phase summaries or tests.

## Known Bugs

**AI logs are global across farms after authorization succeeds:**
- Symptoms: `backend/api/src/main/java/com/yoloFarm/api/service/AiAnalysisService.java:59` copies every in-memory log and returns it after only validating ownership of the requested farm. Logs do not carry `farmId` or `ownerId`.
- Files: `backend/api/src/main/java/com/yoloFarm/api/service/AiAnalysisService.java`
- Trigger: User with access to one farm calls `GET /api/v1/farms/{farmId}/ai-logs` after analyses were submitted for other farms in the same JVM.
- Workaround: None in code. Persist logs with farm/user ownership and filter by `farmId`.

**Device approval does not pre-warm the MQTT feed cache despite having an API for it:**
- Symptoms: `backend/api/src/main/java/com/yoloFarm/api/service/mqtt/MqttReceiverService.java:293` exposes `cacheFeedKey`, but `backend/api/src/main/java/com/yoloFarm/api/service/DeviceService.java:236` only evicts the key after approval and comments that the first MQTT message will warm the cache.
- Files: `backend/api/src/main/java/com/yoloFarm/api/service/DeviceService.java`, `backend/api/src/main/java/com/yoloFarm/api/service/mqtt/MqttReceiverService.java`
- Trigger: A newly approved device publishes immediately after approval.
- Workaround: First message usually falls back to a database lookup; call `cacheFeedKey(resolvedFeedKey, saved)` after save if the intent is immediate warmup.

**Simulator runtime changes only on database NOTIFY or periodic startup/listen loop:**
- Symptoms: `simulator/digital-twin/main.py:224` syncs once at startup, then `simulator/digital-twin/main.py:270` only calls `_sync_once()` when a `device_events` notification is received.
- Files: `simulator/digital-twin/main.py`, `backend/api/src/main/java/com/yoloFarm/api/service/DeviceService.java`
- Trigger: Any direct database update or backend path that changes active devices without sending `NOTIFY device_events`.
- Workaround: Restart the simulator or ensure every device lifecycle mutation sends a notification.

## Security Considerations

**Credentialed CORS trusts all Vercel subdomains:**
- Risk: `backend/api/src/main/java/com/yoloFarm/api/config/CorsConfig.java:18` allows `https://*.vercel.app` while `backend/api/src/main/java/com/yoloFarm/api/config/CorsConfig.java:21` enables credentials.
- Files: `backend/api/src/main/java/com/yoloFarm/api/config/CorsConfig.java`
- Current mitigation: Specific localhost and production origins are also listed at `backend/api/src/main/java/com/yoloFarm/api/config/CorsConfig.java:13`.
- Recommendations: Restrict previews to project-owned domains, move allowed origins to environment configuration, and add an integration test that rejects unrelated Vercel preview origins.

**JWT is stored in localStorage:**
- Risk: `frontend/src/stores/authStore.ts:27` writes the access token to `localStorage`, while `frontend/src/lib/websocket.ts:56`, `frontend/src/lib/websocket.ts:123`, and `frontend/src/lib/websocket.ts:160` reuse it for WebSocket `Authorization` headers.
- Files: `frontend/src/stores/authStore.ts`, `frontend/src/lib/axios.ts`, `frontend/src/lib/websocket.ts`
- Current mitigation: Backend uses stateless JWT validation in `backend/api/src/main/java/com/yoloFarm/api/security/JwtAuthenticationFilter.java` and WebSocket subscription authorization in `backend/api/src/main/java/com/yoloFarm/api/security/WebSocketAuthChannelInterceptor.java`.
- Recommendations: Prefer short-lived access tokens with refresh-token rotation in an HttpOnly cookie, or at minimum reduce token lifetime and enforce a strict CSP.

**Default admin credentials are seeded in code and migrations:**
- Risk: `backend/api/src/main/java/com/yoloFarm/api/config/DataInitializer.java:47` encodes a hard-coded admin password and `backend/api/src/main/resources/db/migration/V2__seed_data.sql:6` documents the default password.
- Files: `backend/api/src/main/java/com/yoloFarm/api/config/DataInitializer.java`, `backend/api/src/main/resources/db/migration/V2__seed_data.sql`
- Current mitigation: Passwords are BCrypt-encoded before storage.
- Recommendations: Gate admin seeding behind a development profile, require first-run credential configuration from environment variables, and remove default credential comments from committed migrations.

**Rate limiting is local, IP-only, and unbounded:**
- Risk: `backend/api/src/main/java/com/yoloFarm/api/security/RateLimitFilter.java:22` stores buckets in a JVM-local `ConcurrentHashMap`, keyed by `request.getRemoteAddr()` at `backend/api/src/main/java/com/yoloFarm/api/security/RateLimitFilter.java:37`.
- Files: `backend/api/src/main/java/com/yoloFarm/api/security/RateLimitFilter.java`
- Current mitigation: Login attempts are limited to five requests per minute per observed remote address.
- Recommendations: Use a distributed store such as Redis for production, honor trusted proxy headers only behind known proxies, expire unused buckets, and apply limits to registration and password-changing endpoints as well.

## Performance Bottlenecks

**Telemetry queries are unbounded and lack supporting indexes in the initial schema:**
- Problem: `backend/api/src/main/java/com/yoloFarm/api/service/TelemetryService.java:42` loads all rows between arbitrary start/end values, and `backend/api/src/main/java/com/yoloFarm/api/repository/TelemetryDataRepository.java:13` returns a full `List`.
- Files: `backend/api/src/main/java/com/yoloFarm/api/service/TelemetryService.java`, `backend/api/src/main/java/com/yoloFarm/api/repository/TelemetryDataRepository.java`, `backend/api/src/main/resources/db/migration/V1__init_schema.sql`
- Cause: `backend/api/src/main/resources/db/migration/V1__init_schema.sql:69` creates `telemetry_data` without an index on `(device_id, created_at)` and the API has no enforced page size or max date range.
- Improvement path: Add a Flyway migration for `(device_id, created_at)`, enforce a maximum query range, support pagination/downsampling, and keep aggregation in SQL for large ranges.

**MQTT observer backpressure can move work onto the callback thread:**
- Problem: `backend/api/src/main/java/com/yoloFarm/api/service/mqtt/MqttReceiverService.java:46` caps the queue at 1000, and `backend/api/src/main/java/com/yoloFarm/api/service/mqtt/MqttReceiverService.java:61` uses `CallerRunsPolicy`.
- Files: `backend/api/src/main/java/com/yoloFarm/api/service/mqtt/MqttReceiverService.java`, `backend/api/src/main/java/com/yoloFarm/api/service/mqtt/observer/DatabaseLoggerObserver.java`, `backend/api/src/main/java/com/yoloFarm/api/service/mqtt/observer/RuleEngineObserver.java`
- Cause: When observers fall behind, database writes and rule evaluation can execute on the MQTT callback path.
- Improvement path: Track queue depth and rejected executions, define drop/coalescing behavior for telemetry bursts, and separate persistence from control-rule evaluation queues.

**Adafruit HTTP client has no explicit timeout policy:**
- Problem: `backend/api/src/main/java/com/yoloFarm/api/service/impl/AdafruitApiServiceImpl.java:33` constructs `new RestTemplate()` and all calls use blocking `exchange` at `backend/api/src/main/java/com/yoloFarm/api/service/impl/AdafruitApiServiceImpl.java:55`, `backend/api/src/main/java/com/yoloFarm/api/service/impl/AdafruitApiServiceImpl.java:97`, and `backend/api/src/main/java/com/yoloFarm/api/service/impl/AdafruitApiServiceImpl.java:133`.
- Files: `backend/api/src/main/java/com/yoloFarm/api/service/impl/AdafruitApiServiceImpl.java`
- Cause: Default client settings do not document connect/read timeout or retry behavior.
- Improvement path: Provide a configured HTTP client bean with bounded connect/read timeouts, retry policy for safe operations, metrics, and tests using a mock HTTP server.

## Fragile Areas

**Automation state is process-local:**
- Files: `backend/api/src/main/java/com/yoloFarm/api/service/automation/AutomationRuntimeStateService.java`, `backend/api/src/main/java/com/yoloFarm/api/service/automation/AutoIrrigationSafetyService.java`, `backend/api/src/main/java/com/yoloFarm/api/service/mqtt/observer/RuleEngineObserver.java`
- Why fragile: `backend/api/src/main/java/com/yoloFarm/api/service/automation/AutomationRuntimeStateService.java:12` and `backend/api/src/main/java/com/yoloFarm/api/service/automation/AutomationRuntimeStateService.java:13` use JVM-local maps for auto-on timers and cooldowns; restarts or multiple backend replicas lose or split automation state.
- Safe modification: Keep current cleanup calls in `backend/api/src/main/java/com/yoloFarm/api/service/DeviceService.java:322` and `backend/api/src/main/java/com/yoloFarm/api/service/RuleService.java:148`, then move state to a shared store before horizontal scaling.
- Test coverage: Unit tests exist for automation safety in `backend/api/src/test/java/com/yoloFarm/api/AutoIrrigationSafetyServiceTest.java` and `backend/api/src/test/java/com/yoloFarm/api/service/automation/AutoIrrigationSafetyServiceTest.java`, but no multi-instance or restart behavior is covered.

**Device and rule services are large orchestration classes:**
- Files: `backend/api/src/main/java/com/yoloFarm/api/service/DeviceService.java`, `backend/api/src/main/java/com/yoloFarm/api/service/RuleService.java`
- Why fragile: `DeviceService.java` is 347 lines and handles ownership, moderation, feed key normalization, Adafruit calls, notifications, cache eviction, and Postgres `NOTIFY`. `RuleService.java` is 357 lines and owns validation, complementary-rule pairing, activation, update, and device mode cleanup.
- Safe modification: Extract feed provisioning, device moderation workflow, and rule-pair validation into focused collaborators only when changing those behaviors; keep current service tests as regression anchors.
- Test coverage: Backend tests cover rename/removal approval and rule pairing in `backend/api/src/test/java/com/yoloFarm/api/DeviceServiceRenameSyncTest.java`, `backend/api/src/test/java/com/yoloFarm/api/DeviceServiceRemovalApprovalTest.java`, `backend/api/src/test/java/com/yoloFarm/api/RuleServiceConditionPairingTest.java`, and `backend/api/src/test/java/com/yoloFarm/api/RuleServiceSchedulePairingTest.java`.

**WebSocket clients are duplicated by channel type:**
- Files: `frontend/src/lib/websocket.ts`
- Why fragile: `frontend/src/lib/websocket.ts:58`, `frontend/src/lib/websocket.ts:125`, and `frontend/src/lib/websocket.ts:162` each create separate STOMP clients with repeated token/header/reconnect logic.
- Safe modification: Extract a small `createStompClient` helper that centralizes broker URL, auth headers, debug behavior, and reconnect policy while keeping per-channel subscriptions explicit.
- Test coverage: No frontend test currently covers WebSocket connection lifecycle or subscription cleanup.

**Simulator concurrency is thread-per-device:**
- Files: `simulator/digital-twin/main.py`
- Why fragile: `simulator/digital-twin/main.py:195` creates one daemon thread per active device, and `_sync_once` can stop/restart runtimes when profile or device metadata changes.
- Safe modification: Keep locking around `_runtimes`; add tests around `_sync_once`, `_stop_runtime`, and command handling before changing thread lifecycle.
- Test coverage: No simulator tests are present under `simulator/digital-twin/`.

## Scaling Limits

**Telemetry storage grows without retention or partitioning:**
- Current capacity: Not defined in code.
- Limit: `backend/api/src/main/resources/db/migration/V1__init_schema.sql:69` creates one `telemetry_data` table with no retention policy, partitioning, or rollup tables.
- Scaling path: Add retention policy, time-based partitioning, aggregate tables for chart views, and indexes matching query paths in `backend/api/src/main/java/com/yoloFarm/api/repository/TelemetryDataRepository.java`.

**Backend horizontal scaling is constrained by in-memory state:**
- Current capacity: Single JVM assumptions for rate limits, AI logs, MQTT feed cache, and automation runtime state.
- Limit: `backend/api/src/main/java/com/yoloFarm/api/security/RateLimitFilter.java:22`, `backend/api/src/main/java/com/yoloFarm/api/service/AiAnalysisService.java:25`, `backend/api/src/main/java/com/yoloFarm/api/service/mqtt/MqttReceiverService.java:66`, and `backend/api/src/main/java/com/yoloFarm/api/service/automation/AutomationRuntimeStateService.java:12` each keep operational state in memory.
- Scaling path: Persist user-visible data, move rate limits and automation timers to shared storage, and define one active MQTT receiver or a partitioned consumer strategy.

**Adafruit free-feed assumptions are baked into error handling:**
- Current capacity: `backend/api/src/main/java/com/yoloFarm/api/service/impl/AdafruitApiServiceImpl.java:64` treats some 422 responses as feed-limit failures.
- Limit: Account feed limits and API rate limits can block device approval when farms grow.
- Scaling path: Model external quota as configuration, add admin-visible quota errors, and support multiple Adafruit accounts or an internal MQTT broker when production load exceeds Adafruit limits.

## Dependencies at Risk

**Paho MQTT client is an older, callback-style dependency:**
- Risk: `backend/api/pom.xml` pins `org.eclipse.paho.client.mqttv3` to `1.2.5`.
- Impact: MQTT behavior depends on callback threading and manual resubscription logic in `backend/api/src/main/java/com/yoloFarm/api/service/mqtt/MqttReceiverService.java`.
- Migration plan: Encapsulate MQTT receive/send behind interfaces already started in `backend/api/src/main/java/com/yoloFarm/api/service/mqtt/MqttSenderService.java`, then evaluate a maintained MQTT client before adding advanced reconnect/backpressure behavior.

**Frontend uses latest-major React Router and React together with limited test coverage:**
- Risk: `frontend/package.json` uses React 19 and React Router 7, while only three frontend test files exist.
- Impact: Route guards, layout, and component behavior can break during dependency updates without broad test coverage.
- Migration plan: Add route-level tests for `frontend/src/App.tsx`, `frontend/src/components/guards/ProtectedRoute.tsx`, and critical farm/device pages before dependency upgrades.

**Spring Boot 4 dependency surface is new relative to many ecosystem examples:**
- Risk: `backend/api/pom.xml` uses Spring Boot `4.0.4`.
- Impact: Security, validation, MVC, and test starter behavior may differ from Spring Boot 3 examples, making copy-pasted fixes risky.
- Migration plan: Prefer official Spring Boot 4 docs and existing project tests; add focused integration tests when changing security or MVC configuration.

## Missing Critical Features

**No real AI provider or storage integration:**
- Problem: `backend/api/src/main/java/com/yoloFarm/api/service/AiAnalysisService.java` returns mock analysis and mock image URLs.
- Blocks: Production disease/pest/health image analysis, auditability of model outputs, and reliable farm-specific analysis history.

**No password reset, email verification, or account recovery flow detected:**
- Problem: Auth exposes register/login/me in `backend/api/src/main/java/com/yoloFarm/api/controller/AuthController.java`, but no recovery or verification endpoints are present.
- Blocks: Production account lifecycle and support workflows.

**No CI workflow detected for automated tests:**
- Problem: `.github/` exists but no workflow file appeared in the tracked file scan.
- Blocks: Pull requests can merge without running Maven/Vitest checks.

## Test Coverage Gaps

**Frontend test coverage is narrow:**
- What's not tested: Most pages and components under `frontend/src/pages` and `frontend/src/components`, including auth pages, admin pages, WebSocket lifecycle, device modals, and rule forms.
- Files: `frontend/src/pages/auth/LoginPage.tsx`, `frontend/src/pages/auth/RegisterPage.tsx`, `frontend/src/pages/admin/AdminDashboardPage.tsx`, `frontend/src/components/devices/ActuatorCard.tsx`, `frontend/src/components/rules/RuleFormModal.tsx`, `frontend/src/lib/websocket.ts`
- Risk: UI regressions, broken API payloads, and route guard bugs can ship unnoticed.
- Priority: High

**Simulator has no automated tests:**
- What's not tested: Profile merging, database sync, actuator command handling, MQTT publish payload formatting, and device thread lifecycle.
- Files: `simulator/digital-twin/main.py`, `simulator/digital-twin/tools/e2e_create_device_and_verify.py`
- Risk: Simulator changes can silently stop telemetry generation or actuator feedback during demos and local integration testing.
- Priority: Medium

**Security configuration lacks contract tests for CORS and WebSocket authorization edge cases:**
- What's not tested: Rejection of unrelated origins, credentialed preview origins, missing WebSocket tokens, malformed farm topic subscriptions, and non-owner topic subscriptions.
- Files: `backend/api/src/main/java/com/yoloFarm/api/config/CorsConfig.java`, `backend/api/src/main/java/com/yoloFarm/api/security/WebSocketAuthChannelInterceptor.java`, `backend/api/src/test/java/com/yoloFarm/api/ApiEndpointContractTest.java`
- Risk: Security regressions may appear only in browser or WebSocket clients after deployment.
- Priority: High

**External API failure handling is not covered with HTTP-level tests:**
- What's not tested: Adafruit create/rename/delete timeout, 401/403, 404, 422, and retry/idempotency behavior.
- Files: `backend/api/src/main/java/com/yoloFarm/api/service/impl/AdafruitApiServiceImpl.java`, `backend/api/src/test/java/com/yoloFarm/api/DeviceServiceRenameSyncTest.java`, `backend/api/src/test/java/com/yoloFarm/api/DeviceServiceRemovalApprovalTest.java`
- Risk: Device approval/removal flows can leave remote and local state inconsistent.
- Priority: High

---

*Concerns audit: 2026-05-18*
