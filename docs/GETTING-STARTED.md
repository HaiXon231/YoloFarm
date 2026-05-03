# Getting Started

Tai lieu huong dan chay he thong local (Windows PowerShell).

## 1. Yeu cau

- Java 21
- Node.js 18+ va npm
- Python 3.10+
- PostgreSQL
- Adafruit IO account

## 2. Tao bien moi truong

### Backend

Tao `backend/api/.env` tu `backend/api/.env.example`:

- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET_KEY`
- `ADAFRUIT_USERNAME`
- `ADAFRUIT_IO_KEY`

Tuy chon:

- `ADAFRUIT_BROKER_URL` (mặc định `ssl://io.adafruit.com:8883`)
- `WS_ALLOWED_ORIGINS` (mặc định `http://localhost:3000`)

### Simulator

Tao `simulator/digital-twin/.env` tu `simulator/digital-twin/.env.example`:

- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`
- `ADAFRUIT_USERNAME`, `ADAFRUIT_IO_KEY`, `ADAFRUIT_BROKER`, `ADAFRUIT_PORT`

Dam bao co `simulator/digital-twin/profiles.json` (copy tu `profiles.example.json`).

## 3. Chay nhanh

Mo 3 terminal tai repo root:

### Terminal 1: Backend

```powershell
.\scripts\run-backend.ps1
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

## 4. Kiem tra nhanh

- Frontend: `http://localhost:3000`
- Backend: `http://localhost:8080`
- API prefix: `/api/v1`

## 5. Lenh ho tro

```powershell
.\scripts\status-local.ps1
.\scripts\stop-all-local.ps1
```
