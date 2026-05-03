# YoloFarm

Nền tảng nông nghiệp thông minh với 3 khối chính:

- **Backend API (Spring Boot):** xác thực, quản lý farm/thiết bị, rule engine, lưu telemetry, nhận MQTT, đẩy WebSocket.
- **Frontend Web App (React + Vite):** dashboard và UI quản trị cho admin/farmer.
- **Digital Twin Simulator (Python):** mô phỏng sensor/actuator, publish telemetry lên Adafruit IO.

## Kiến trúc tổng quan

- Frontend chạy tại `http://localhost:3000`
- Backend chạy tại `http://localhost:8080`
- Frontend gọi backend qua Vite proxy (`/api`, `/ws`)
- Backend nhận telemetry từ Adafruit MQTT và lưu vào PostgreSQL
- Simulator đọc device ACTIVE trong DB và publish dữ liệu lên Adafruit

## Cấu trúc repo

- `backend/api`: Spring Boot backend + OpenAPI spec
- `frontend`: React + TypeScript frontend
- `simulator/digital-twin`: Python digital twin simulator
- `scripts`: script hỗ trợ chạy local

## Yêu cầu môi trường

- Windows PowerShell (script `.ps1`)
- Java 21 (xem `backend/api/pom.xml`)
- Node.js 18+ và npm
- Python 3.10+
- PostgreSQL
- Tài khoản Adafruit IO (username + key)

## Thiết lập môi trường

### 1) Backend env

Tạo `backend/api/.env` từ `backend/api/.env.example`:

- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET_KEY`
- `ADAFRUIT_USERNAME`
- `ADAFRUIT_IO_KEY`

Tùy chọn:

- `ADAFRUIT_BROKER_URL` (mặc định: `ssl://io.adafruit.com:8883`)
- `WS_ALLOWED_ORIGINS` (mặc định: `http://localhost:3000`)

### 2) Frontend env (tùy chọn)

`frontend/.env.example` dùng cho override, hiện tại frontend dùng proxy trong Vite.

### 3) Simulator env

Tạo `simulator/digital-twin/.env` từ `simulator/digital-twin/.env.example`:

- DB (`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`)
- Adafruit (`ADAFRUIT_USERNAME`, `ADAFRUIT_IO_KEY`, `ADAFRUIT_BROKER`, `ADAFRUIT_PORT`)

Đảm bảo có `simulator/digital-twin/profiles.json` (copy từ `profiles.example.json`).

## Chạy nhanh (khuyến nghị)

Mở 3 terminal tại repo root (`d:\YoloFarm`).

### Terminal 1: Backend

```powershell
.\scripts\run-backend.ps1
```

Truyền env file khác nếu cần:

```powershell
.\scripts\run-backend.ps1 -EnvFile "backend\api\.env"
```

### Terminal 2: Frontend

```powershell
cd frontend
npm install
npm run dev
```

### Terminal 3: Simulator

```powershell
.\scripts\run-simulator.ps1
```

Các flag hữu ích:

```powershell
.\scripts\run-simulator.ps1 -InitOnly
.\scripts\run-simulator.ps1 -ApplyFeedKeys
.\scripts\run-simulator.ps1 -ApplyFeedKeys -RewriteAllFeedKeys
```

## Thao tác local

```powershell
.\scripts\status-local.ps1
.\scripts\stop-all-local.ps1
```

## API & Contract

- OpenAPI: `backend/api/openAPI.yaml`
- Base URL: `http://localhost:8080`
- API prefix: `/api/v1`

## Build & Test nhanh

```powershell
cd backend/api
.\mvnw.cmd test
```

```powershell
cd frontend
npm run build
```

```powershell
cd simulator/digital-twin
.\.venv\Scripts\python.exe -m py_compile main.py tools\feed_key_manager.py
```

## Feed key (Adafruit)

- Regex: `^[a-z0-9-]{1,64}$`
- Backend có logic alias để hỗ trợ `-` và `_` trong MQTT topic.

## Quy ước bảo mật

- Không commit `.env`
- Thông tin nhạy cảm chỉ nằm ở local

## Tài liệu chi tiết

- `docs/ARCHITECTURE.md`
- `docs/GETTING-STARTED.md`
- `docs/DEVELOPMENT.md`
- `docs/TESTING.md`
- `docs/CONFIGURATION.md`
- `docs/PROJECT-DETAILS.md` (tài liệu tổng hợp phục vụ phỏng vấn)
- Simulator: `simulator/digital-twin/README.md`
