# YoloFarm

YoloFarm is a smart-farming platform with three main parts:

- Backend API (Spring Boot): authentication, farm/device management, rules, telemetry storage, MQTT ingestion, WebSocket push.
- Frontend Web App (React + Vite): dashboard and management UI for admin/farmer workflows.
- Digital Twin Simulator (Python): simulates sensors/actuators and publishes telemetry to Adafruit IO.

## Architecture

- Frontend runs on `http://localhost:3000`
- Backend runs on `http://localhost:8080`
- Frontend calls backend via Vite proxy (`/api`, `/ws`)
- Backend receives telemetry from Adafruit MQTT and stores it in PostgreSQL
- Simulator reads active devices from DB, simulates device behavior, and sends data to Adafruit

## Repository Structure

- `backend/api`: Spring Boot backend + OpenAPI spec
- `frontend`: React + TypeScript frontend
- `simulator/digital-twin`: Python digital twin simulator
- `scripts`: root-level helper scripts for local run/stop/status

## Prerequisites

- Windows PowerShell (scripts are `.ps1`)
- Java 25 (see `backend/api/pom.xml`)
- Node.js 18+ and npm
- Python 3.10+
- PostgreSQL
- Adafruit IO account (`username` and `key`)

## Environment Setup

### 1) Backend env

Create `backend/api/.env` from `backend/api/.env.example` and fill values:

- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET_KEY`
- `ADAFRUIT_USERNAME`
- `ADAFRUIT_IO_KEY`

Optional:

- `ADAFRUIT_BROKER_URL` (default: `ssl://io.adafruit.com:8883`)
- `WS_ALLOWED_ORIGINS` (default: `http://localhost:3000`)

### 2) Frontend env (optional)

There is `frontend/.env.example` for future local overrides. Current frontend setup already uses local proxy in Vite config.

### 3) Simulator env

Create `simulator/digital-twin/.env` from `simulator/digital-twin/.env.example` and fill values:

- DB connection (`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`)
- Adafruit (`ADAFRUIT_USERNAME`, `ADAFRUIT_IO_KEY`, `ADAFRUIT_BROKER`, `ADAFRUIT_PORT`)

Also ensure `simulator/digital-twin/profiles.json` exists (copy from `profiles.example.json` if needed).

## Quick Start (Recommended)

Open separate terminals from repository root (`d:\YoloFarm`).

### Terminal 1: Start backend

```powershell
.\scripts\run-backend.ps1
```

You can provide a custom env file path:

```powershell
.\scripts\run-backend.ps1 -EnvFile "backend\api\.env"
```

### Terminal 2: Start frontend

```powershell
cd frontend
npm install
npm run dev
```

### Terminal 3: Start simulator

```powershell
.\scripts\run-simulator.ps1
```

Useful simulator flags:

```powershell
# Prepare venv/deps/config only
.\scripts\run-simulator.ps1 -InitOnly

# Generate and apply missing feed keys
.\scripts\run-simulator.ps1 -ApplyFeedKeys

# Rewrite all feed keys then apply
.\scripts\run-simulator.ps1 -ApplyFeedKeys -RewriteAllFeedKeys
```

## Local Operations

Check current local status:

```powershell
.\scripts\status-local.ps1
```

Stop backend (port 8080) and simulator process:

```powershell
.\scripts\stop-all-local.ps1
```

## API and Contract

- OpenAPI spec: `backend/api/openAPI.yaml`
- Backend base URL: `http://localhost:8080`
- Main API prefix: `/api/v1`

## Build and Test

### Backend

```powershell
cd backend/api
.\mvnw.cmd test
```

### Frontend

```powershell
cd frontend
npm run build
```

### Simulator sanity check

```powershell
cd simulator/digital-twin
.\.venv\Scripts\python.exe -m py_compile main.py tools\feed_key_manager.py
```

## Feed Key Rules (Current)

Backend validation enforces Adafruit-compatible feed key format:

- lowercase letters, digits, and dash only
- regex: `^[a-z0-9-]{1,64}$`

MQTT ingestion also includes alias fallback logic for `-` and `_` differences to improve compatibility with legacy feed aliases.

## Security and Git Hygiene

- Do not commit real `.env` files.
- Keep credentials only in local environment files.
- Generated folders like `frontend/node_modules`, `frontend/dist`, Python cache, and build output are ignored by `.gitignore`.

## Troubleshooting

- Backend fails on startup with missing env vars:
  - Verify `backend/api/.env` exists and includes required keys.
- Frontend cannot call API:
  - Confirm backend is running on `localhost:8080`.
  - Confirm frontend is running with `npm run dev` (uses Vite proxy).
- Simulator does not publish data:
  - Verify Adafruit credentials in simulator `.env`.
  - Verify device is ACTIVE and has an `adafruit_feed_key` in DB.

## Additional Docs

- Simulator details: `simulator/digital-twin/README.md`
