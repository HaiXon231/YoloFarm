# Project Details (Tai lieu phong van)

Tai lieu nay tong hop toan bo qua trinh: y tuong, thiet ke, hien thuc, cau hinh va file quan trong trong source code. Muc tieu la giup giai thich ro rang khi phong van.

## 1. Y tuong & bai toan

**Bai toan:** Quan ly nong trai thong minh voi sensor/actuator, can theo doi telemetry realtime, dieu khien thu cong hoac tu dong theo rule, va co mo phong de test khi khong co phan cung that.

**Muc tieu chinh:**
- Quan ly farm, device, rule (CRUD).
- Nhan telemetry tu IoT (MQTT), luu DB, hien thi realtime.
- Dieu khien thiet bi qua MQTT.
- Tu dong hoa theo nguong va theo lich.
- Co simulator doc lap de mo phong va test.

## 2. Tong quan kien truc

**3 khoi chinh:**
- Backend Spring Boot (nghiep vu + MQTT + WebSocket).
- Frontend React (UI dashboard).
- Digital Twin Python (mo phong thiet bi).

Lien ket:
- Backend <-> PostgreSQL (JPA/JDBC)
- Backend <-> Adafruit IO (MQTT + REST)
- Frontend <-> Backend (REST + WebSocket)
- Simulator <-> DB + Adafruit

Tai lieu chi tiet: `docs/ARCHITECTURE.md`

## 3. Key design patterns

### 3.1 Observer Pattern (MQTT -> xu ly)

- Subject: `MqttReceiverService`
- Observers:
  - `DatabaseLoggerObserver`: luu telemetry
  - `RuleEngineObserver`: rule engine
  - `WebSocketNotifierObserver`: push realtime

PlantUML: `backend/plantUML/design_pattern_backend.puml`

### 3.2 Strategy Pattern (dieu khien thiet bi)

- Context: `IrrigationContext`
- Strategy: `ManualStrategy`, `AutoThresholdStrategy`, `ScheduledStrategy`

Folder: `backend/api/src/main/java/com/yoloFarm/api/service/strategy/`

## 4. Luong xu ly quan trong

### 4.1 Telemetry flow

1. Adafruit publish MQTT `{username}/feeds/{feed_key}`
2. `MqttReceiverService` nhan message, map feed -> device
3. Tao `SensorData`, goi `notifyObservers`
4. DB logger luu, WS notifier push

File lien quan:
- `backend/api/src/main/java/com/yoloFarm/api/service/mqtt/MqttReceiverService.java`
- `backend/api/src/main/java/com/yoloFarm/api/service/mqtt/observer/DatabaseLoggerObserver.java`
- `backend/api/src/main/java/com/yoloFarm/api/service/mqtt/observer/WebSocketNotifierObserver.java`

### 4.2 Dieu khien thiet bi

1. Frontend goi REST /api/v1/devices/{deviceId}/command
2. `ControlService` goi `IrrigationContext`
3. Strategy publish MQTT

File lien quan:
- `backend/api/src/main/java/com/yoloFarm/api/service/strategy/ManualStrategy.java`
- `backend/api/src/main/java/com/yoloFarm/api/service/strategy/AutoThresholdStrategy.java`
- `backend/api/src/main/java/com/yoloFarm/api/service/strategy/ScheduledStrategy.java`

### 4.3 Tu dong hoa (Rule engine)

1. `RuleEngineObserver` nhan SensorData
2. Kiem tra dieu kien + cooldown
3. Thuc thi auto command
4. Ghi notification

File lien quan:
- `backend/api/src/main/java/com/yoloFarm/api/service/mqtt/observer/RuleEngineObserver.java`
- `backend/api/src/main/java/com/yoloFarm/api/service/automation/AutomationRuntimeStateService.java`

### 4.4 Offline detection

- `DeviceHeartbeatService` chay theo lich, mark OFFLINE neu qua 5 phut khong co signal

File: `backend/api/src/main/java/com/yoloFarm/api/service/DeviceHeartbeatService.java`

## 5. Cac module quan trong va vai tro

### Backend

- **Security:** JWT + role-based auth
  - `backend/api/src/main/java/com/yoloFarm/api/config/SecurityConfig.java`
  - `backend/api/src/main/java/com/yoloFarm/api/security/JwtAuthenticationFilter.java`
  - `backend/api/src/main/java/com/yoloFarm/api/security/WebSocketAuthChannelInterceptor.java`

- **CORS:**
  - `backend/api/src/main/java/com/yoloFarm/api/config/CorsConfig.java`

- **MQTT setup:**
  - `backend/api/src/main/java/com/yoloFarm/api/config/MqttConfig.java`

- **Rate limit login:**
  - `backend/api/src/main/java/com/yoloFarm/api/security/RateLimitFilter.java`

- **Rule scheduler:**
  - `backend/api/src/main/java/com/yoloFarm/api/service/RuleSchedulerService.java`

- **Auto-off safety:**
  - `backend/api/src/main/java/com/yoloFarm/api/service/automation/AutoIrrigationSafetyService.java`

### Frontend

- Router + guard: `frontend/src/App.tsx`, `frontend/src/components/guards/`
- Auth store: `frontend/src/stores/authStore.ts`
- Notification store (WS + REST): `frontend/src/stores/notificationStore.ts`
- Axios config: `frontend/src/lib/axios.ts`
- Vite proxy: `frontend/vite.config.ts`

### Simulator

- Core runtime: `simulator/digital-twin/main.py`
- Profile config: `simulator/digital-twin/profiles.json`
- Env setup: `simulator/digital-twin/.env`

## 6. Cau hinh & file quan trong

- Backend config: `backend/api/src/main/resources/application.yml`
- Backend env: `backend/api/.env`
- Frontend env (optional): `frontend/.env`
- Simulator env: `simulator/digital-twin/.env`
- OpenAPI: `backend/api/openAPI.yaml`

Tai lieu chi tiet: `docs/CONFIGURATION.md`

## 7. Script va thao tac local

- Run backend: `scripts/run-backend.ps1`
- Run simulator: `scripts/run-simulator.ps1`
- Status: `scripts/status-local.ps1`
- Stop: `scripts/stop-all-local.ps1`

## 8. Thach thuc & giai phap

### 8.1 MQTT reconnect + subscribe

- Paho auto reconnect, can re-subscribe khi mat ket noi.
- Xu ly trong `MqttReceiverService` (`connectionLost` + `connectComplete`).

### 8.2 Feed key alias

- Adafruit co the khac dau gach `-`/`_`, backend map alias trong `findDeviceByFeedAlias`.

### 8.3 Rule pairing ON/OFF

- Rule bat buoc co cap ON/OFF, validate trong `RuleService`.

### 8.4 Safety auto-off

- `AutoIrrigationSafetyService` gioi han thoi gian auto ON.

## 9. Testing

- Backend: `./mvnw test`
- Frontend: `npx vitest` (chua co script `test`)
- Simulator: `py_compile` cho `main.py`

Tai lieu chi tiet: `docs/TESTING.md`

## 10. Huong trinh bay phong van (goi y)

- 30s: Gioi thieu bai toan + 3 khoi chinh.
- 1-2 phut: Trinh bay luong MQTT -> DB -> WebSocket (Observer).
- 1-2 phut: Trinh bay Rule engine + Strategy.
- 1 phut: Trinh bay Simulator va gia tri when no hardware.
- 30s: Nêu thach thuc (MQTT reconnect, rule pairing, safety).

## 11. Huong phat trien tiep

- Dong goi docker compose (backend + db + simulator)
- Tang test coverage frontend/simulator
- Hoan thien AI analysis (hien dang stub)
