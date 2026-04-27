# STACK.md — Technology Stack

**Last mapped:** 2026-04-27

---

## Languages

| Layer | Language | Version |
|---|---|---|
| Backend | Java | 25 (LTS) |
| Frontend | TypeScript | ~5.7.0 |
| Simulator | Python | 3.x |
| Build scripts | PowerShell | 5.1+ |

---

## Runtime & Frameworks

### Backend — `backend/api/`
- **Spring Boot** `4.0.4` (parent POM)
  - `spring-boot-starter-webmvc` — REST API layer (Jackson SNAKE_CASE strategy)
  - `spring-boot-starter-websocket` — STOMP over SockJS for real-time push
  - `spring-boot-starter-data-jpa` — ORM via Hibernate (`ddl-auto: update`)
  - `spring-boot-starter-security` — JWT-based stateless auth
  - `spring-boot-starter-validation` — Bean Validation (Jakarta)
  - `spring-boot-starter-actuator` — health / metrics endpoints
- **Eclipse Paho MQTT** `1.2.5` — MQTT client for Adafruit IO connection
- **JJWT** `0.11.5` — JWT signing/parsing (api + impl + jackson runtime)
- **Lombok** — compile-time boilerplate reduction (`@Builder`, `@Slf4j`, etc.)
- **Jackson Databind** — JSON serialization; globally configured to `SNAKE_CASE`
- **Build tool:** Maven Wrapper (`mvnw` / `mvnw.cmd`)

### Frontend — `frontend/`
- **React** `^19.0.0` — SPA, component tree
- **Vite** `^6.0.0` — build tool & dev server
- **TypeScript** `~5.7.0`
- **react-router-dom** `^7.1.0` — client-side routing with role guards
- **Zustand** `^5.0.0` — lightweight global state management
- **@stomp/stompjs** `^7.0.0` + **sockjs-client** `^1.6.1` — WebSocket/STOMP client
- **axios** `^1.7.0` — HTTP client for REST calls
- **recharts** `^2.15.0` — telemetry charts
- **react-hot-toast** `^2.4.1` — toast notifications
- **date-fns** `^4.1.0` — date formatting
- **Tailwind CSS** `^3.4.17` — utility-first CSS (PostCSS/autoprefixer pipeline)

### Simulator — `simulator/digital-twin/`
- **Python** standard library — `threading`, `signal`, `select`, `ssl`, `json`
- **paho-mqtt** `1.6.1` — MQTT client for publishing/subscribing feeds
- **psycopg2-binary** `2.9.10` — direct PostgreSQL connection (raw SQL)
- **python-dotenv** `1.1.0` — env var loading

---

## Database

| Component | Technology |
|---|---|
| Production DB | PostgreSQL 14+ (`yolofarm_db`) |
| Test DB | H2 (in-memory, runtime scope) |
| ORM | Hibernate (JPA) — `ddl-auto: update` |
| Schema | Auto-managed by Hibernate; SQL init scripts in `backend/api/sql/` and `simulator/digital-twin/sql/` |

---

## Configuration

### Backend (`backend/api/.env.example` → `backend/api/.env`)
```
DB_USERNAME           — PostgreSQL user
DB_PASSWORD           — PostgreSQL password
JWT_SECRET_KEY        — Base64 JWT signing secret (HS256)
ADAFRUIT_USERNAME     — Adafruit IO account username
ADAFRUIT_IO_KEY       — Adafruit IO API key
ADAFRUIT_BROKER_URL   — MQTT broker URL (default: ssl://io.adafruit.com:8883)
WS_ALLOWED_ORIGINS    — CORS origins for WebSocket (default: http://localhost:3000)
AUTOMATION_RULE_COOLDOWN_SECONDS  — Rule cooldown (default: 30)
AUTOMATION_MAX_AUTO_ON_MINUTES    — Safety auto-off threshold (default: 20)
AUTOMATION_AUTO_OFF_WATCHDOG_INTERVAL_MS — Watchdog poll interval (default: 30000)
```

### Frontend (`frontend/.env`)
```
VITE_API_BASE_URL     — Backend REST base URL
VITE_WS_URL           — WebSocket endpoint URL
```

### Simulator (`simulator/digital-twin/.env`)
```
DB_HOST / DB_PORT / DB_NAME / DB_USER / DB_PASSWORD — PostgreSQL connection
ADAFRUIT_USERNAME / ADAFRUIT_IO_KEY / ADAFRUIT_BROKER / ADAFRUIT_PORT
SYNC_SECONDS          — DB sync interval (default: 15)
SIM_PROFILES_FILE     — Path to simulation profiles JSON (default: profiles.json)
LOG_LEVEL             — Logging level (default: INFO)
```

---

## Key Dependencies Summary

```xml
<!-- Backend pom.xml core deps -->
spring-boot-starter-parent       4.0.4
spring-boot-starter-webmvc
spring-boot-starter-websocket
spring-boot-starter-data-jpa
spring-boot-starter-security
spring-boot-starter-validation
spring-boot-starter-actuator
org.eclipse.paho:client.mqttv3   1.2.5
io.jsonwebtoken:jjwt-api         0.11.5
org.postgresql:postgresql        (runtime)
com.h2database:h2                (test)
org.projectlombok:lombok         (compile-time)
```

---

## DevOps / Scripts

| Script | Purpose |
|---|---|
| `scripts/run-backend.ps1` | Start Spring Boot backend locally |
| `scripts/run-simulator.ps1` | Start digital twin simulator |
| `scripts/status-local.ps1` | Check local service statuses |
| `scripts/stop-all-local.ps1` | Gracefully stop all local services |
