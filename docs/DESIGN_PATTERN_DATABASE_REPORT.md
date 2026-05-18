# YoloFarm Design Patterns, Database Design, and Core Components Report

## 1. Muc dich tai lieu

Tai lieu nay tong hop cach YoloFarm duoc thiet ke o 3 lop chinh:

- **Design patterns** dang duoc su dung trong backend Spring Boot.
- **Database design** va cac quan he giua cac entity chinh.
- **Core components** dieu phoi telemetry, automation, control, notification, va realtime push.

Muc tieu la giup ban co mot ban mo ta co cau truc ro rang de dung khi review code, onboarding, va phong van.

## 2. Tong quan kien truc

YoloFarm gom 3 khoi chinh:

- **Backend API (Spring Boot):** xu ly nghiep vu, auth, MQTT, WebSocket, JPA, scheduler.
- **Frontend Web (React + Vite):** dashboard cho admin va farmer.
- **Digital Twin Simulator (Python):** mo phong sensor/actuator, publish telemetry, doc DB de phat lenh.

Lien ket he thong:

- Frontend -> Backend qua REST va WebSocket.
- Backend -> PostgreSQL qua JPA/JDBC.
- Backend -> Adafruit IO qua MQTT TLS.
- Simulator -> PostgreSQL va Adafruit IO.

Xem them:

- [docs/ARCHITECTURE.md](ARCHITECTURE.md)
- [README.md](../README.md)

## 3. Design patterns dang duoc su dung

### 3.1 Observer Pattern

**Vi tri:** `backend/api/src/main/java/com/yoloFarm/api/service/mqtt/` va `backend/api/src/main/java/com/yoloFarm/api/service/mqtt/observer/`

**Y nghia trong project:** `MqttReceiverService` dong vai tro Subject. Moi khi co MQTT message tu Adafruit, service nay tao `SensorData` va phat sang cac observer.

**Cac observer chinh:**

- `DatabaseLoggerObserver`: luu telemetry vao DB.
- `RuleEngineObserver`: kiem tra rule va kich hoat automation.
- `WebSocketNotifierObserver`: day du lieu realtime len frontend.

**Vi sao hop ly:**

- Tach luong nhan telemetry ra khoi luong xu ly.
- Cho phep them observer moi ma khong can sua logic MQTT trung tam.
- Giam coupling giua ingestion va cac hanh dong tiep theo.

**File neo:**

- [MqttReceiverService.java](../backend/api/src/main/java/com/yoloFarm/api/service/mqtt/MqttReceiverService.java)
- [RuleEngineObserver.java](../backend/api/src/main/java/com/yoloFarm/api/service/mqtt/observer/RuleEngineObserver.java)
- [TelemetryObserversTest.java](../backend/api/src/test/java/com/yoloFarm/api/TelemetryObserversTest.java)
- [RuleEngineObserverTest.java](../backend/api/src/test/java/com/yoloFarm/api/service/mqtt/observer/RuleEngineObserverTest.java)

**Diem ky thuat dang luu y:**

- Observer duoc chay qua bounded executor trong `MqttReceiverService` de tranh dua task vao common pool vo han.
- Neu queue day, service quay ve caller thread thay vi mat message.
- Threshold check tren `minValue` va `maxValue` hien tai chi log warning, khong chan telemetry.

### 3.2 Strategy Pattern

**Vi tri:** `backend/api/src/main/java/com/yoloFarm/api/service/strategy/`

**Y nghia trong project:** `IrrigationContext` nhan mot strategy va thuc thi lenh dieu khien device. Logic dieu khien thay doi theo boi canh, nhung API goi ra van giong nhau.

**Cac strategy chinh:**

- `ManualStrategy`: dieu khien thu cong.
- `AutoThresholdStrategy`: dieu khien theo nguong telemetry.
- `ScheduledStrategy`: dieu khien theo lich.

**Vi sao hop ly:**

- Nhom nhieu cach dieu khien thanh cung mot contract.
- Tach rule decision ra khoi lenh publish MQTT.
- Dung cho both rule engine va scheduler.

**File neo:**

- [IrrigationContext.java](../backend/api/src/main/java/com/yoloFarm/api/service/strategy/IrrigationContext.java)
- [ManualStrategy.java](../backend/api/src/main/java/com/yoloFarm/api/service/strategy/ManualStrategy.java)
- [AutoThresholdStrategy.java](../backend/api/src/main/java/com/yoloFarm/api/service/strategy/AutoThresholdStrategy.java)
- [ScheduledStrategy.java](../backend/api/src/main/java/com/yoloFarm/api/service/strategy/ScheduledStrategy.java)

### 3.3 Scheduler Pattern

**Vi tri:** cac service co `@Scheduled` trong backend.

**Cac scheduler chinh:**

- `RuleSchedulerService`: quet rule theo cron moi phut.
- `DeviceHeartbeatService`: danh dau thiet bi OFFLINE neu qua nguong timeout.
- `AutoIrrigationSafetyService`: watchdog tu dong tat AUTO neu chay qua gioi han.

**Vi sao hop ly:**

- Dua cac tac vu nhip lap ra khoi request-response path.
- Phu hop voi automation, heartbeat, va safety watchdog.

**File neo:**

- [RuleSchedulerService.java](../backend/api/src/main/java/com/yoloFarm/api/service/RuleSchedulerService.java)
- [DeviceHeartbeatService.java](../backend/api/src/main/java/com/yoloFarm/api/service/DeviceHeartbeatService.java)
- [AutoIrrigationSafetyService.java](../backend/api/src/main/java/com/yoloFarm/api/service/automation/AutoIrrigationSafetyService.java)

### 3.4 Repository Pattern

**Vi tri:** `backend/api/src/main/java/com/yoloFarm/api/repository/`

**Y nghia trong project:** Spring Data JPA repositories dong vai tro trung gian giua domain object va PostgreSQL.

**Diem dang chu y:**

- Repository khong chi CRUD ma con co query custom, `JOIN FETCH`, projection, va bulk update.
- Nhiều method duoc dat ten theo convention de Spring sinh query tu dong.
- Query custom duoc dung de tranh N+1 va LazyInitializationException trong MQTT/scheduler context.

**File neo:**

- [DeviceRepository.java](../backend/api/src/main/java/com/yoloFarm/api/repository/DeviceRepository.java)
- [RuleRepository.java](../backend/api/src/main/java/com/yoloFarm/api/repository/RuleRepository.java)

### 3.5 Singleton / Managed Bean Pattern

Trong Spring, nhieu service va config class duoc quan ly nhu singleton bean mac dinh:

- `MqttReceiverService`
- `RuleEngineObserver`
- `IrrigationContext`
- `AutomationRuntimeStateService`

**Gia tri:**

- Duy tri state dung cap instance khi can.
- Cho phep inject dependency ro rang.
- Giam viec tu khoi tao object trong nhieu noi.

### 3.6 Event-Driven / Callback Style

Project nay con co mot lop thiet ke theo callback/event:

- MQTT callback (`MqttCallbackExtended`) trong `MqttReceiverService`.
- WebSocket push khi telemetry/device status thay doi.
- Scheduler trigger theo cron/fixed delay.

Day khong phai mot pattern rieng, nhung la truc dieu khien core cua he thong.

## 4. Database design

### 4.1 Tong quan

He thong dung **PostgreSQL** la database chinh. Schema duoc quan ly bang **Flyway**.

- Init schema: [V1__init_schema.sql](../backend/api/src/main/resources/db/migration/V1__init_schema.sql)
- Seed data: [V2__seed_data.sql](../backend/api/src/main/resources/db/migration/V2__seed_data.sql)

### 4.2 Cac bang chinh

#### `users`

Chua thong tin account va quyen truy cap.

Cot chinh:

- `id` UUID
- `username` unique
- `email` unique
- `password`
- `role`
- `created_at`

Entity:

- [User.java](../backend/api/src/main/java/com/yoloFarm/api/entity/User.java)

#### `farms`

Dai dien cho mot nong trai/owner scope.

Cot chinh:

- `id` UUID
- `owner_id` -> `users.id`
- `name`
- `location`
- `created_at`

Entity:

- [Farm.java](../backend/api/src/main/java/com/yoloFarm/api/entity/Farm.java)

#### `models`

Danh muc device model.

Cot chinh:

- `id` UUID
- `model_name`
- `manufacturer`
- `device_type` (SENSOR/ACTUATOR)
- `metric_type`

Entity:

- [DeviceModel.java](../backend/api/src/main/java/com/yoloFarm/api/entity/DeviceModel.java)

#### `devices`

La bang trung tam cua heap device.

Cot chinh:

- `id` UUID
- `farm_id` -> `farms.id`
- `model_id` -> `models.id`
- `name`
- `status`
- `adafruit_feed_key` unique
- `connection_status`
- `last_seen`
- `operating_mode`
- `is_active`
- `min_value`
- `max_value`

Entity:

- [Device.java](../backend/api/src/main/java/com/yoloFarm/api/entity/Device.java)

#### `rules`

Luu automation rule.

Cot chinh:

- `id` UUID
- `farm_id` -> `farms.id`
- `trigger_device_id` -> `devices.id` (co the null)
- `action_device_id` -> `devices.id`
- `rule_type`
- `rule_name`
- `operator`
- `threshold_value`
- `cron_expression`
- `action_command`
- `is_active`

Entity:

- [Rule.java](../backend/api/src/main/java/com/yoloFarm/api/entity/Rule.java)

#### `telemetry_data`

Luu du lieu telemetry tinh toan/lau dai.

Cot chinh:

- `id` UUID
- `device_id`
- `metric_type`
- `value`
- `created_at`

Entity:

- [TelemetryData.java](../backend/api/src/main/java/com/yoloFarm/api/entity/TelemetryData.java)

#### `notifications`

Luu thong bao cho user.

Cot chinh:

- `id` UUID
- `user_id` -> `users.id`
- `message`
- `is_read`
- `created_at`

Entity:

- [Notification.java](../backend/api/src/main/java/com/yoloFarm/api/entity/Notification.java)

### 4.3 Quan he giua cac bang

#### Nhan vat trung tam

- **User** co nhieu **Farm**.
- **Farm** co nhieu **Device** va nhieu **Rule**.
- **DeviceModel** duoc tai su dung boi nhieu **Device**.
- **Rule** tham chieu 2 device: mot device trigger va mot device action.
- **TelemetryData** tham chieu mot `device_id` logic.
- **Notification** thuoc ve mot `user`.

#### Mo ta ERD logic

- `users 1 -> N farms`
- `farms 1 -> N devices`
- `farms 1 -> N rules`
- `models 1 -> N devices`
- `devices 1 -> N telemetry_data` theo `device_id` logic
- `users 1 -> N notifications`
- `rules` co hai FK den `devices`: trigger va action

### 4.4 Dac diem thiet ke DB

#### UUID la khoa chinh

He thong chon UUID cho tat ca entity chinh:

- Giup an danh ID
- Tot cho scale va generate client-independent
- Phu hop voi distributed/IoT context

#### Enum storage la `STRING`

Nhiều cot dung `@Enumerated(EnumType.STRING)`:

- `role`
- `device_type`
- `metric_type`
- `status`
- `connection_status`
- `operating_mode`
- `rule_type`
- `action_command`

Loi ich:

- Doc de hieu.
- On dinh hon so voi ordinal khi enum thay doi.

#### Lazy loading + JOIN FETCH

Cac repository custom hay dung `JOIN FETCH` de tranh loi lazy trong:

- MQTT callback
- Scheduler
- xu ly ngoai transactional context

Day la mot quyet dinh quan trong trong project nay.

### 4.5 Seed data

`V2__seed_data.sql` tao san:

- 1 admin account
- tap model cho sensor/actuator

Muc tieu:

- Co du lieu khoi dong de test local nhanh.
- Khong phai tao tay truoc moi lan chay.

## 5. Core components va vai tro

### 5.1 MQTT ingestion core

**Lop trung tam:** `MqttReceiverService`

Nhiem vu:

- Nhan MQTT message tu Adafruit IO.
- Parse topic `username/feeds/feedKey`.
- Resolve feed key alias.
- Update trang thai device va `lastSeen`.
- Phat `SensorData` ra observer.
- Push WebSocket khi device ONLINE/OFFLINE thay doi.

**File:**

- [MqttReceiverService.java](../backend/api/src/main/java/com/yoloFarm/api/service/mqtt/MqttReceiverService.java)

### 5.2 Rule engine core

**Lop trung tam:** `RuleEngineObserver`

Nhiem vu:

- Doc rule active co lien quan den device.
- Danh gia dieu kien `operator` + `thresholdValue`.
- Kiem tra cooldown.
- Goi `IrrigationContext` + strategy phu hop.
- Tao notification khi auto action thanh cong.

**File:**

- [RuleEngineObserver.java](../backend/api/src/main/java/com/yoloFarm/api/service/mqtt/observer/RuleEngineObserver.java)

### 5.3 Command execution core

**Lop trung tam:** `IrrigationContext`

Nhiem vu:

- Thu hut cac cach dieu khien khac nhau vao mot contract thong nhat.
- Tranh race condition bang cach truyen strategy qua tham so.

### 5.4 Runtime state core

**Lop trung tam:** `AutomationRuntimeStateService`

Nhiem vu:

- Ghi nhan command da chay.
- Giữ state `auto-on-since`.
- Hỗ trợ cooldown cho rule.
- Cleanup state khi rule/device bi xoa.

### 5.5 Scheduler core

- `RuleSchedulerService` chay rule theo cron.
- `DeviceHeartbeatService` chuyen device thanh OFFLINE neu stale.
- `AutoIrrigationSafetyService` tat AUTO neu chay qua gioi han.

## 6. Cac query repository dang chu y

### DeviceRepository

- `findByAdafruitFeedKeyIgnoreCaseWithModelAndFarm`
- `findStaleOnlineDevices`
- `markStaleDevicesAsOffline`
- `findActiveAutoActuatorsWithFarmAndOwner`
- `findAdminDeviceSummaries`

### RuleRepository

- `findActiveRulesWithAssociations`
- `findActiveScheduledRulesWithAssociations`
- `deleteRulesBoundToDevice`
- `findRuleNamesBoundToDevice`

**Ly do quan trong:**

- Giam N+1 query.
- Dam bao data day du trong scheduled/MQTT context.
- Khong bi lazy loading error ngoai transaction.

## 7. Cach he thong xu ly nghiep vu

### 7.1 Telemetry flow

1. Adafruit publish message theo topic `username/feeds/feedKey`.
2. `MqttReceiverService` nhan va map sang `Device`.
3. `SensorData` duoc phat sang observer.
4. `DatabaseLoggerObserver` luu telemetry.
5. `WebSocketNotifierObserver` day data realtime.
6. `RuleEngineObserver` kiem tra automation rule.

### 7.2 Manual control flow

1. Frontend goi REST command.
2. `ControlService` chon strategy phu hop.
3. `IrrigationContext` goi `ManualStrategy`.
4. Strategy publish len Adafruit IO.

### 7.3 Schedule automation flow

1. `RuleSchedulerService` quet cron rule.
2. Neu den thoi diem, service goi `ScheduledStrategy`.
3. He thong ghi nhan runtime state va tao notification.

### 7.4 Safety watchdog flow

1. `AutoIrrigationSafetyService` tim device dang AUTO.
2. Neu qua `maxAutoOnMinutes`, no goi OFF.
3. Tao notification an toan.

## 8. Danh gia kien truc

### Diem manh

- Pattern ro rang, de mo rong.
- Database schema tinh gọn nhung phu hop IoT domain.
- Dung `JOIN FETCH` dung cho cac luong ngoai transaction.
- Co runtime state service de giam bug do stale state.

### Diem can chu y

- Cac service scheduler va MQTT callback rat nhay voi lazy loading, nen phai giu repository query chat.
- `TelemetryData` dang luu `device_id` nhu UUID logic, chua map thanh entity relation.
- `minValue` va `maxValue` hien tai chi la warning, khong chan dong telemetry.

## 9. Ket luan

YoloFarm la mot backend Spring Boot co thiet ke kha “thuc chien” cho IoT:

- **Observer** dung de phan tach ingestion va xu ly.
- **Strategy** dung de thay doi cach dieu khien device.
- **Scheduler** dung de chay automation va safety watchdog.
- **Repository + JOIN FETCH** dung de dam bao hieu nang va an toan trong transaction boundary.
- **Flyway + PostgreSQL** tao nen schema on dinh, co kieu du lieu ro rang, phu hop voi telemetry va rule engine.

Neu can, ban co the mo rong tai lieu nay thanh:

- ban “review phong van” ngan gon 1-2 trang,
- ban “thuyet trinh technical design” co so do,
- hoac ban “architecture slide notes” de dien thuyet.
