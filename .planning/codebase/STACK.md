# STACK.md — Technology Stack (Backend)

**Last mapped:** 2026-04-28

---

## Scope
- `backend/api/` (Spring Boot API)

---

## Languages

| Layer | Language | Version |
|---|---|---|
| Backend | Java | 21 |
| Build scripts | PowerShell | 5.1+ |

---

## Runtime & Frameworks

### Spring Boot — `backend/api/`
- **Spring Boot** `4.0.4` (parent POM)
  - `spring-boot-starter-webmvc` — REST API layer (Jackson SNAKE_CASE strategy)
  - `spring-boot-starter-websocket` — STOMP over SockJS for real-time push
  - `spring-boot-starter-data-jpa` — ORM via Hibernate (`ddl-auto: update`)
  - `spring-boot-starter-security` — JWT-based stateless auth
  - `spring-boot-starter-validation` — Bean Validation (Jakarta)
  - `spring-boot-starter-actuator` — health/metrics endpoints
- **Flyway** — DB migrations (`src/main/resources/db/migration`)
- **Bucket4j** `8.10.1` — login rate limiting (filter)
- **Eclipse Paho MQTT** `1.2.5` — MQTT client for Adafruit IO connection
- **JJWT** `0.11.5` — JWT signing/parsing (api + impl + jackson runtime)
- **Lombok** — compile-time boilerplate reduction (`@Builder`, `@Slf4j`, etc.)
- **Jackson Databind** — JSON serialization; globally configured to `SNAKE_CASE`
- **Build tool:** Maven Wrapper (`mvnw` / `mvnw.cmd`)

---

## Database

| Component | Technology |
|---|---|
| Primary DB | PostgreSQL (JDBC) |
| Test DB | H2 (runtime scope) |
| ORM | Hibernate (JPA) |
| Migrations | Flyway (`src/main/resources/db/migration`) |

---

## Configuration

### Backend (`backend/api/.env.example` → `backend/api/.env`)
```
DB_USERNAME
DB_PASSWORD
JWT_SECRET_KEY
ADAFRUIT_USERNAME
ADAFRUIT_IO_KEY
ADAFRUIT_BROKER_URL   (optional)
WS_ALLOWED_ORIGINS    (optional)
```

### Application defaults (`backend/api/src/main/resources/application.yml`)
- `DB_URL` default: `jdbc:postgresql://localhost:5432/yolofarm_db`
- `app.automation.*` defaults for rule cooldown, auto-off, watchdog interval

---

## Key Dependencies Summary

```xml
spring-boot-starter-parent       4.0.4
spring-boot-starter-webmvc
spring-boot-starter-websocket
spring-boot-starter-data-jpa
spring-boot-starter-security
spring-boot-starter-validation
spring-boot-starter-actuator
org.flywaydb:flyway-core
org.flywaydb:flyway-database-postgresql
com.bucket4j:bucket4j-core       8.10.1
org.eclipse.paho:client.mqttv3   1.2.5
io.jsonwebtoken:jjwt-*           0.11.5
org.postgresql:postgresql        (runtime)
com.h2database:h2                (runtime)
org.projectlombok:lombok         (compile-time)
```

---

## Dev Scripts

| Script | Purpose |
|---|---|
| `backend/api/scripts/run-local.ps1` | Start Spring Boot backend locally |
| `scripts/run-simulator.ps1` | Start digital twin simulator |
| `scripts/status-local.ps1` | Check local service statuses |
| `scripts/stop-all-local.ps1` | Gracefully stop all local services |
