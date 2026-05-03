# Kien truc he thong YoloFarm

## 1. Tong quan thanh phan

YoloFarm gom 3 khoi chinh:

- **Backend API (Spring Boot):** xu ly nghiep vu, luu tru, MQTT, WebSocket.
- **Frontend Web (React + Vite):** giao dien dashboard cho admin/farmer.
- **Digital Twin (Python):** mo phong sensor/actuator va publish MQTT.

Lien ket cao cap:

```
[Frontend] --HTTP/WS--> [Backend] --JDBC/JPA--> [PostgreSQL]
                               \
                                \--MQTT TLS--> [Adafruit IO]
[Simulator] --JDBC--> [PostgreSQL]
[Simulator] --MQTT TLS--> [Adafruit IO]
```

## 2. Backend layer

**Entry point:** `backend/api/src/main/java/com/yoloFarm/api/ApiApplication.java`

- **Controller layer:** `backend/api/src/main/java/com/yoloFarm/api/controller/`
- **Service layer:** `backend/api/src/main/java/com/yoloFarm/api/service/`
- **Repository layer:** `backend/api/src/main/java/com/yoloFarm/api/repository/`
- **Security:** `backend/api/src/main/java/com/yoloFarm/api/security/`

### 2.1 Mqtt + Observer pattern

- **Subject:** `MqttReceiverService` nhan message tu Adafruit.
- **Observers:**
  - `DatabaseLoggerObserver` luu telemetry.
  - `RuleEngineObserver` xu ly rule tu dong.
  - `WebSocketNotifierObserver` day realtime cho frontend.

PlantUML: `backend/plantUML/design_pattern_backend.puml`

### 2.2 Strategy pattern cho dieu khien

- `IrrigationContext` goi strategy theo tinh huong.
- `ManualStrategy`: lenh thu cong.
- `AutoThresholdStrategy`: rule theo nguong.
- `ScheduledStrategy`: rule theo lich.

Folder: `backend/api/src/main/java/com/yoloFarm/api/service/strategy/`

### 2.3 Scheduler & Safety

- `DeviceHeartbeatService`: danh dau OFFLINE neu mat ket noi.
- `RuleSchedulerService`: chay rule theo cron.
- `AutoIrrigationSafetyService`: auto-off theo gioi han thoi gian.

Folder: `backend/api/src/main/java/com/yoloFarm/api/service/`

## 3. Cac luong xu ly chinh

### 3.1 Telemetry

1. Adafruit publish MQTT: `{username}/feeds/{feed_key}`
2. `MqttReceiverService` nhan payload, map feed -> device
3. Tao `SensorData` va goi `notifyObservers`
4. Luu DB + push WebSocket

### 3.2 Dieu khien thiet bi

1. Frontend goi REST `/api/v1/devices/{deviceId}/command`
2. `ControlService` goi `IrrigationContext.executeControl`
3. `MqttSenderService` publish MQTT len Adafruit

### 3.3 Tu dong hoa (Rule)

1. `RuleEngineObserver` nhan `SensorData`
2. Kiem tra dieu kien + cooldown
3. Thuc thi `AutoThresholdStrategy`
4. Luu thong bao qua `NotificationService`

## 4. WebSocket

- Endpoint: `/ws` (SockJS)
- Topic chinh: `/topic/farm/{farmId}/telemetry`
- Auth: `WebSocketAuthChannelInterceptor`

## 5. Luu tru

- PostgreSQL, Hibernate JPA
- Flyway migration: `backend/api/src/main/resources/db/migration/`

## 6. Tai lieu lien quan

- API spec: `backend/api/openAPI.yaml`
- Config: `backend/api/src/main/resources/application.yml`
