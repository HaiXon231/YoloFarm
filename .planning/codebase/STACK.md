# Technology Stack

**Analysis Date:** 2026-05-18

## Languages

**Primary:**
- Java 21 target - Spring Boot backend in `backend/api/src/main/java/com/yoloFarm/api/`, configured by `backend/api/pom.xml`.
- TypeScript 5.7 - React frontend in `frontend/src/`, configured by `frontend/tsconfig.json`.

**Secondary:**
- Python 3.10+ - Digital twin simulator in `simulator/digital-twin/main.py` and helper tools in `simulator/digital-twin/tools/`.
- SQL - PostgreSQL schema and seed migrations in `backend/api/src/main/resources/db/migration/`; simulator hardening SQL in `simulator/digital-twin/sql/001_unique_adafruit_feed_key.sql`.
- PowerShell - Local run scripts in `scripts/`, `backend/api/scripts/run-local.ps1`, and `simulator/digital-twin/scripts/run-simulator.ps1`.

## Runtime

**Environment:**
- Backend: Java runtime for Spring Boot. `backend/api/pom.xml` sets `<java.version>21</java.version>`, while GitHub Actions currently run Java 25 in `.github/workflows/ci.yml` and `.github/workflows/deploy-backend-ssh.yml`.
- Frontend: Node.js 18+ per `README.md`; CI uses Node 22 in `.github/workflows/ci.yml`; local environment reported Node `v24.11.1` and npm `11.6.2`.
- Simulator: Python 3.10+ per `README.md` and `simulator/digital-twin/README.md`; local environment reported Python `3.13.0`.

**Package Manager:**
- Backend: Maven wrapper in `backend/api/mvnw` and `backend/api/mvnw.cmd`; wrapper config `backend/api/.mvn/wrapper/maven-wrapper.properties` resolves Maven `3.9.14`.
- Frontend: npm with lockfile `frontend/package-lock.json` using lockfileVersion `3`.
- Simulator: pip with `simulator/digital-twin/requirements.txt`.
- Lockfile: Frontend lockfile present; backend Maven wrapper present; Python requirements file present.

## Frameworks

**Core:**
- Spring Boot `4.0.4` - Backend API, security, JPA, WebSocket, scheduling, and actuator support via `backend/api/pom.xml`.
- React `^19.0.0` - Frontend UI in `frontend/src/App.tsx` and route/pages under `frontend/src/pages/`.
- Vite `^6.0.0` - Frontend dev server/build tooling in `frontend/vite.config.ts`.
- Tailwind CSS `^3.4.17` - Design tokens and utility styling in `frontend/tailwind.config.ts` and `frontend/src/index.css`.
- Python script runtime - Simulator service entry point in `simulator/digital-twin/main.py`.

**Testing:**
- Spring Boot Test - Backend tests under `backend/api/src/test/java/com/yoloFarm/api/`, configured by `backend/api/pom.xml`.
- H2 runtime test database - Backend test profile in `backend/api/src/test/resources/application.yml`.
- Vitest `^4.1.5` with jsdom `^29.1.0` - Frontend tests configured in `frontend/vite.config.ts` and setup in `frontend/src/setupTests.ts`.
- Testing Library - Frontend component/store tests under `frontend/src/**/__tests__/`.

**Build/Dev:**
- Maven Spring Boot plugin - Backend package/run lifecycle in `backend/api/pom.xml`.
- Docker multi-stage build - Backend container in `backend/api/Dockerfile` using Maven builder and Eclipse Temurin 21 JRE runtime.
- Vite React plugin `@vitejs/plugin-react` - Frontend React transform in `frontend/vite.config.ts`.
- PostCSS/Autoprefixer - CSS pipeline files `frontend/postcss.config.js` and `frontend/package.json`.
- GitHub Actions - CI and deployment workflows in `.github/workflows/ci.yml`, `.github/workflows/deploy-backend-ssh.yml`, and `.github/workflows/deploy-frontend-pages.yml`.

## Key Dependencies

**Critical:**
- `spring-boot-starter-webmvc` - REST API controllers under `backend/api/src/main/java/com/yoloFarm/api/controller/`.
- `spring-boot-starter-security` - JWT-secured API and role rules in `backend/api/src/main/java/com/yoloFarm/api/config/SecurityConfig.java`.
- `spring-boot-starter-data-jpa` + `org.postgresql:postgresql` - Persistence through entities in `backend/api/src/main/java/com/yoloFarm/api/entity/` and repositories in `backend/api/src/main/java/com/yoloFarm/api/repository/`.
- `org.flywaydb:flyway-core` + `flyway-database-postgresql` - Database migrations in `backend/api/src/main/resources/db/migration/`.
- `org.eclipse.paho.client.mqttv3:1.2.5` - Backend MQTT publish/subscribe integration in `backend/api/src/main/java/com/yoloFarm/api/config/MqttConfig.java` and `backend/api/src/main/java/com/yoloFarm/api/service/mqtt/`.
- `io.jsonwebtoken:jjwt-* 0.11.5` - JWT generation/validation in `backend/api/src/main/java/com/yoloFarm/api/service/security/JwtService.java`.
- `axios ^1.7.0` - Frontend REST client in `frontend/src/lib/axios.ts`.
- `@stomp/stompjs ^7.0.0` + `sockjs-client ^1.6.1` - Frontend realtime client in `frontend/src/lib/websocket.ts`.
- `zustand ^5.0.0` - Frontend auth/notification stores in `frontend/src/stores/`.
- `paho-mqtt==1.6.1` - Simulator MQTT client in `simulator/digital-twin/main.py`.
- `psycopg2-binary==2.9.10` - Simulator PostgreSQL access in `simulator/digital-twin/main.py` and `simulator/digital-twin/tools/feed_key_manager.py`.

**Infrastructure:**
- `com.bucket4j:bucket4j-core 8.10.1` - Rate limiting support in `backend/api/src/main/java/com/yoloFarm/api/security/RateLimitFilter.java`.
- `org.projectlombok:lombok` - Backend boilerplate reduction across entities/services/configs in `backend/api/src/main/java/com/yoloFarm/api/`.
- `react-router-dom ^7.1.0` - Frontend routing in `frontend/src/App.tsx`.
- `recharts ^2.15.0` - Frontend charting dependency used by telemetry views under `frontend/src/components/telemetry/`.
- `date-fns ^4.1.0` - Frontend date formatting dependency.
- `react-hot-toast ^2.4.1` - Frontend notifications dependency.
- `python-dotenv==1.1.0` - Simulator local environment loading in `simulator/digital-twin/main.py`.

## Configuration

**Environment:**
- Backend environment is read through Spring placeholders in `backend/api/src/main/resources/application.yml`.
- Required backend env vars: `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET_KEY`, `ADAFRUIT_USERNAME`, `ADAFRUIT_IO_KEY`.
- Optional backend env vars: `DB_URL`, `ADAFRUIT_BROKER_URL`, `WS_ALLOWED_ORIGINS`, `AUTOMATION_RULE_COOLDOWN_SECONDS`, `AUTOMATION_MAX_AUTO_ON_MINUTES`, `AUTOMATION_AUTO_OFF_WATCHDOG_INTERVAL_MS`.
- Frontend environment overrides use `VITE_API_URL` in `frontend/src/lib/axios.ts` and `VITE_WS_URL` in `frontend/src/lib/websocket.ts`; Vite proxy defaults route `/api` and `/ws` to `http://localhost:8080` in `frontend/vite.config.ts`.
- Simulator environment uses `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `ADAFRUIT_USERNAME`, `ADAFRUIT_IO_KEY`, `ADAFRUIT_BROKER`, `ADAFRUIT_PORT`, `SYNC_SECONDS`, and `SIM_PROFILES_FILE` as documented in `docs/CONFIGURATION.md` and `simulator/digital-twin/README.md`.
- Secret-bearing files exist at `backend/api/.env`, `backend/api/.env.example`, `frontend/.env`, `frontend/.env.example`, `simulator/digital-twin/.env`, and `simulator/digital-twin/.env.example`; do not read or quote their contents.

**Build:**
- Backend build config: `backend/api/pom.xml`, `backend/api/.mvn/wrapper/maven-wrapper.properties`, `backend/api/Dockerfile`.
- Backend application config: `backend/api/src/main/resources/application.yml`, `backend/api/src/main/resources/application.properties`.
- Frontend build config: `frontend/package.json`, `frontend/package-lock.json`, `frontend/vite.config.ts`, `frontend/tsconfig.json`, `frontend/tailwind.config.ts`, `frontend/postcss.config.js`, `frontend/vercel.json`.
- Simulator config: `simulator/digital-twin/requirements.txt`, `simulator/digital-twin/profiles.json`, `simulator/digital-twin/profiles.example.json`.
- CI/deploy config: `.github/workflows/ci.yml`, `.github/workflows/deploy-backend-ssh.yml`, `.github/workflows/deploy-frontend-pages.yml`.

## Platform Requirements

**Development:**
- Run backend from `backend/api` with `.\mvnw.cmd spring-boot:run` or `scripts/run-backend.ps1`; `backend/api/scripts/run-local.ps1` loads a local env file into process environment.
- Run frontend from `frontend` with `npm install` and `npm run dev`; dev server uses port `3000` in `frontend/vite.config.ts`.
- Run simulator from `simulator/digital-twin` with a virtual environment and `python main.py`, or use `scripts/run-simulator.ps1`.
- Local dependencies: PostgreSQL database, Adafruit IO account/key, Java runtime, Node/npm, Python/pip, Windows PowerShell scripts.

**Production:**
- Backend can be packaged as a jar via Maven or containerized with `backend/api/Dockerfile`; Docker runtime exposes port `7860` and sets `SERVER_PORT=7860` for HuggingFace Spaces compatibility.
- Frontend deploy target includes static hosting with SPA rewrites in `frontend/vercel.json`; `.github/workflows/deploy-frontend-pages.yml` deploys `frontend/dist` to GitHub Pages.
- Backend SSH deploy workflow builds a jar and restarts `yolofarm-api` via `.github/workflows/deploy-backend-ssh.yml`.
- Simulator is an independent service that must share PostgreSQL and Adafruit credentials with the backend, documented in `simulator/digital-twin/README.md`.

---

*Stack analysis: 2026-05-18*
