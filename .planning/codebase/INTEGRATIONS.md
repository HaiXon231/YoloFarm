# External Integrations

**Analysis Date:** 2026-05-18

## APIs & External Services

**Backend REST API:**
- Spring Boot API - Serves the frontend and external clients with `/api/v1` routes.
  - SDK/Client: `axios` client in `frontend/src/lib/axios.ts`
  - Auth: JWT bearer token stored as `access_token` by `frontend/src/stores/authStore.ts`
  - Contract: `backend/api/openAPI.yaml` and summary docs in `docs/API.md`

**Realtime WebSocket/STOMP:**
- Backend SockJS/STOMP endpoint `/ws` - Pushes telemetry, device status, admin stats, and notification counts.
  - SDK/Client: `@stomp/stompjs` and `sockjs-client` in `frontend/src/lib/websocket.ts`
  - Auth: `Authorization: Bearer <token>` connect header from `frontend/src/lib/websocket.ts`
  - Server config: `backend/api/src/main/java/com/yoloFarm/api/config/WebSocketConfig.java`
  - Server auth: `backend/api/src/main/java/com/yoloFarm/api/security/WebSocketAuthChannelInterceptor.java`
  - Allowed origins: `WS_ALLOWED_ORIGINS` via `backend/api/src/main/resources/application.yml`

**Adafruit IO MQTT:**
- Adafruit MQTT broker - Ingests sensor telemetry and carries actuator commands.
  - SDK/Client: Eclipse Paho Java client in `backend/api/src/main/java/com/yoloFarm/api/config/MqttConfig.java`; Python Paho client in `simulator/digital-twin/main.py`
  - Auth: `ADAFRUIT_USERNAME`, `ADAFRUIT_IO_KEY`, optional `ADAFRUIT_BROKER_URL`; simulator uses `ADAFRUIT_USERNAME`, `ADAFRUIT_IO_KEY`, `ADAFRUIT_BROKER`, `ADAFRUIT_PORT`
  - Subscribe path: backend subscribes to `{username}/feeds/+` in `backend/api/src/main/java/com/yoloFarm/api/service/mqtt/MqttReceiverService.java`
  - Publish path: backend publishes commands to `{username}/feeds/{adafruitFeedKey}` in `backend/api/src/main/java/com/yoloFarm/api/service/impl/MqttSenderServiceImpl.java`
  - Simulator path: simulator publishes telemetry and subscribes for actuator commands in `simulator/digital-twin/main.py`

**Adafruit IO REST API:**
- Adafruit feed management API - Creates, renames, and deletes feeds when devices are approved, renamed, or removed.
  - SDK/Client: Spring `RestTemplate` in `backend/api/src/main/java/com/yoloFarm/api/service/impl/AdafruitApiServiceImpl.java`
  - Auth: `X-AIO-Key` header populated from `ADAFRUIT_IO_KEY`
  - Base URL pattern: `https://io.adafruit.com/api/v2/{username}/feeds`
  - Caller: `backend/api/src/main/java/com/yoloFarm/api/service/DeviceService.java`

**Static Hosting / Deploy Targets:**
- GitHub Pages - Frontend deploy target in `.github/workflows/deploy-frontend-pages.yml`.
  - SDK/Client: GitHub Actions `actions/deploy-pages@v4`
  - Auth: GitHub OIDC/Pages permissions in `.github/workflows/deploy-frontend-pages.yml`
- SSH backend host - Backend deploy target in `.github/workflows/deploy-backend-ssh.yml`.
  - SDK/Client: `appleboy/scp-action@v0.1.7` and `appleboy/ssh-action@v1.0.3`
  - Auth: GitHub Actions secrets `SERVER_HOST`, `SERVER_USER`, `SERVER_SSH_KEY`
- Vercel/static SPA compatibility - SPA rewrites in `frontend/vercel.json`; CORS allows Vercel origins in `backend/api/src/main/java/com/yoloFarm/api/config/CorsConfig.java`.

## Data Storage

**Databases:**
- PostgreSQL - Primary backend database for users, farms, models, devices, rules, telemetry, and notifications.
  - Connection: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` via `backend/api/src/main/resources/application.yml`
  - Client: Spring Data JPA repositories in `backend/api/src/main/java/com/yoloFarm/api/repository/`
  - Driver: `org.postgresql:postgresql` in `backend/api/pom.xml`
  - Migrations: Flyway scripts in `backend/api/src/main/resources/db/migration/`
- PostgreSQL - Simulator reads active devices and applies feed key governance.
  - Connection: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`
  - Client: `psycopg2-binary` in `simulator/digital-twin/main.py` and `simulator/digital-twin/tools/feed_key_manager.py`
  - Hardening SQL: `simulator/digital-twin/sql/001_unique_adafruit_feed_key.sql`
- H2 in-memory database - Backend tests only.
  - Connection: `jdbc:h2:mem:yolofarm_test` in `backend/api/src/test/resources/application.yml`
  - Client: Spring Data JPA test dependencies in `backend/api/pom.xml`

**File Storage:**
- Not detected for production persistence.
- AI image analysis currently returns mock image URLs and keeps logs in memory in `backend/api/src/main/java/com/yoloFarm/api/service/AiAnalysisService.java`.
- Simulator profile files live on local filesystem at `simulator/digital-twin/profiles.json` and `simulator/digital-twin/profiles.example.json`.

**Caching:**
- No external cache service detected.
- Backend MQTT feed lookup cache uses in-memory `ConcurrentHashMap` in `backend/api/src/main/java/com/yoloFarm/api/service/mqtt/MqttReceiverService.java`.
- AI analysis logs use in-memory `CopyOnWriteArrayList` in `backend/api/src/main/java/com/yoloFarm/api/service/AiAnalysisService.java`.

## Authentication & Identity

**Auth Provider:**
- Custom JWT authentication backed by PostgreSQL users.
  - Implementation: `backend/api/src/main/java/com/yoloFarm/api/service/impl/AuthServiceImpl.java`, `backend/api/src/main/java/com/yoloFarm/api/service/security/JwtService.java`, and `backend/api/src/main/java/com/yoloFarm/api/security/JwtAuthenticationFilter.java`
  - Password hashing: `BCryptPasswordEncoder` bean in `backend/api/src/main/java/com/yoloFarm/api/config/ApplicationConfig.java`
  - User details: `backend/api/src/main/java/com/yoloFarm/api/entity/User.java` implements Spring Security `UserDetails`
  - Secret: `JWT_SECRET_KEY`
  - Expiration: `jwt.expiration` in `backend/api/src/main/resources/application.yml`
  - Frontend storage: `access_token` and `role` in localStorage via `frontend/src/stores/authStore.ts`

## Monitoring & Observability

**Error Tracking:**
- No external error tracking service detected.
- Backend exposes Spring Boot actuator dependency through `spring-boot-starter-actuator` in `backend/api/pom.xml`, but no explicit management endpoint configuration is present in `backend/api/src/main/resources/application.yml`.

**Logs:**
- Backend uses SLF4J via Lombok `@Slf4j` in services/configs such as `backend/api/src/main/java/com/yoloFarm/api/config/MqttConfig.java`, `backend/api/src/main/java/com/yoloFarm/api/service/mqtt/MqttReceiverService.java`, and `backend/api/src/main/java/com/yoloFarm/api/service/impl/AdafruitApiServiceImpl.java`.
- Backend logging level for `com.yoloFarm` is configured as `INFO` in `backend/api/src/main/resources/application.yml`.
- Frontend uses `console.log`, `console.warn`, and `console.error` for websocket lifecycle and parse failures in `frontend/src/lib/websocket.ts`.
- Simulator uses Python logging in `simulator/digital-twin/main.py`.

## CI/CD & Deployment

**Hosting:**
- Frontend: GitHub Pages via `.github/workflows/deploy-frontend-pages.yml`; `frontend/vercel.json` also supports Vercel/static SPA rewrites.
- Backend: SSH-managed server workflow in `.github/workflows/deploy-backend-ssh.yml`; Dockerfile `backend/api/Dockerfile` supports container deployment and HuggingFace Spaces-style port `7860`.
- Simulator: No CI/CD workflow detected; documented as a standalone service in `simulator/digital-twin/README.md`.

**CI Pipeline:**
- GitHub Actions CI in `.github/workflows/ci.yml`.
- Backend CI: checkout, setup Java 25, run `./mvnw -B test` from `backend/api`.
- Frontend CI: checkout, setup Node 22, run `npm ci` and `npm run build` from `frontend`.
- No Python simulator CI job detected in `.github/workflows/ci.yml`.

## Environment Configuration

**Required env vars:**
- Backend: `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET_KEY`, `ADAFRUIT_USERNAME`, `ADAFRUIT_IO_KEY`.
- Backend optional: `DB_URL`, `ADAFRUIT_BROKER_URL`, `WS_ALLOWED_ORIGINS`, `AUTOMATION_RULE_COOLDOWN_SECONDS`, `AUTOMATION_MAX_AUTO_ON_MINUTES`, `AUTOMATION_AUTO_OFF_WATCHDOG_INTERVAL_MS`.
- Frontend optional: `VITE_API_URL`, `VITE_WS_URL`.
- Simulator: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `ADAFRUIT_USERNAME`, `ADAFRUIT_IO_KEY`, `ADAFRUIT_BROKER`, `ADAFRUIT_PORT`, `SYNC_SECONDS`, `SIM_PROFILES_FILE`.
- GitHub Actions deploy: `SERVER_HOST`, `SERVER_USER`, `SERVER_SSH_KEY`.

**Secrets location:**
- Local backend env files exist at `backend/api/.env` and `backend/api/.env.example`; contents were not read.
- Local frontend env files exist at `frontend/.env` and `frontend/.env.example`; contents were not read.
- Local simulator env files exist at `simulator/digital-twin/.env` and `simulator/digital-twin/.env.example`; contents were not read.
- GitHub deployment secrets are referenced in `.github/workflows/deploy-backend-ssh.yml`.

## Webhooks & Callbacks

**Incoming:**
- REST API endpoints under `/api/v1` are implemented by controllers in `backend/api/src/main/java/com/yoloFarm/api/controller/` and documented in `backend/api/openAPI.yaml`.
- WebSocket/STOMP clients connect to `/ws` configured by `backend/api/src/main/java/com/yoloFarm/api/config/WebSocketConfig.java`.
- MQTT callback receives Adafruit IO messages through Paho `MqttCallbackExtended` in `backend/api/src/main/java/com/yoloFarm/api/service/mqtt/MqttReceiverService.java`.
- Scheduled jobs run inside the backend in `backend/api/src/main/java/com/yoloFarm/api/service/RuleSchedulerService.java`, `backend/api/src/main/java/com/yoloFarm/api/service/DeviceHeartbeatService.java`, and `backend/api/src/main/java/com/yoloFarm/api/service/automation/AutoIrrigationSafetyService.java`.

**Outgoing:**
- Backend publishes MQTT actuator commands through `backend/api/src/main/java/com/yoloFarm/api/service/impl/MqttSenderServiceImpl.java`.
- Backend calls Adafruit IO feed REST endpoints through `backend/api/src/main/java/com/yoloFarm/api/service/impl/AdafruitApiServiceImpl.java`.
- Backend pushes STOMP messages with `SimpMessagingTemplate` from `backend/api/src/main/java/com/yoloFarm/api/service/mqtt/MqttReceiverService.java`, `backend/api/src/main/java/com/yoloFarm/api/service/mqtt/observer/WebSocketNotifierObserver.java`, and `backend/api/src/main/java/com/yoloFarm/api/service/NotificationService.java`.
- Simulator publishes telemetry and subscribes to actuator command feeds in `simulator/digital-twin/main.py`.

---

*Integration audit: 2026-05-18*
