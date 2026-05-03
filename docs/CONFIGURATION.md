# Configuration

## 1. Backend

### File
- `backend/api/src/main/resources/application.yml`

### Cac bien moi truong

File mau: `backend/api/.env.example`

- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET_KEY`
- `ADAFRUIT_USERNAME`
- `ADAFRUIT_IO_KEY`
- `ADAFRUIT_BROKER_URL` (optional)
- `WS_ALLOWED_ORIGINS` (optional)

### Tham so quan trong trong application.yml

- `spring.datasource.url` (mac dinh `jdbc:postgresql://localhost:5432/yolofarm_db`)
- `spring.jpa.hibernate.ddl-auto: update`
- `spring.jackson.property-naming-strategy: SNAKE_CASE`
- `jwt.secret-key` + `jwt.expiration`
- `app.automation.rule-command-cooldown-seconds`
- `app.automation.max-auto-on-minutes`
- `app.automation.auto-off-watchdog-interval-ms`

### CORS

- REST CORS: `backend/api/src/main/java/com/yoloFarm/api/config/CorsConfig.java`
- WebSocket CORS: `app.websocket.allowed-origins` trong `application.yml`

## 2. Frontend

### Vite proxy

`frontend/vite.config.ts` proxy:

- `/api` -> `http://localhost:8080`
- `/ws` -> `http://localhost:8080` (WebSocket)

### API base

`frontend/src/lib/axios.ts`:

- `baseURL = import.meta.env.VITE_API_URL || '/api/v1'`

## 3. Simulator

### Env file

`simulator/digital-twin/.env`:

- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`
- `ADAFRUIT_USERNAME`, `ADAFRUIT_IO_KEY`, `ADAFRUIT_BROKER`, `ADAFRUIT_PORT`
- `SYNC_SECONDS` (mac dinh 15)
- `SIM_PROFILES_FILE` (mac dinh `profiles.json`)

### Profiles

`simulator/digital-twin/profiles.json`:

- `defaults`, `metrics`, `devices`
- Ho tro override theo `devices[device_id]` va `devices[feed:{feed_key}]`
