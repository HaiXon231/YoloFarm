# Development Guide

Huong dan lam viec va phat trien cho ca 3 khoi (backend, frontend, simulator).

## 1. Cau truc cong viec

- Backend: `backend/api/`
- Frontend: `frontend/`
- Simulator: `simulator/digital-twin/`

## 2. Backend

### Chay local

```powershell
.\scripts\run-backend.ps1
```

### Thu muc quan trong

- Controller: `backend/api/src/main/java/com/yoloFarm/api/controller/`
- Service: `backend/api/src/main/java/com/yoloFarm/api/service/`
- Repository: `backend/api/src/main/java/com/yoloFarm/api/repository/`
- Config: `backend/api/src/main/java/com/yoloFarm/api/config/`
- Security: `backend/api/src/main/java/com/yoloFarm/api/security/`
- OpenAPI: `backend/api/openAPI.yaml`

## 3. Frontend

### Chay local

```powershell
cd frontend
npm install
npm run dev
```

### Router va guard

- Router: `frontend/src/App.tsx`
- ProtectedRoute/PublicRoute: `frontend/src/components/guards/`

### State

- Auth store: `frontend/src/stores/authStore.ts`
- Notification store: `frontend/src/stores/notificationStore.ts`

## 4. Simulator

### Chay local

```powershell
.\scripts\run-simulator.ps1
```

### Logic chinh

- `simulator/digital-twin/main.py` (DigitalTwinManager)
- `simulator/digital-twin/profiles.json` (profile)

## 5. Quy uoc chung

- JSON tra ve dang `snake_case` (backend Jackson config)
- WebSocket topic chinh: `/topic/farm/{farmId}/telemetry`
- MQTT topic: `{username}/feeds/{feed_key}`

## 6. Loi thuong gap

- Thieu `.env` -> backend fail startup
- Frontend khong goi duoc API -> kiem tra Vite proxy o `frontend/vite.config.ts`
- Simulator khong publish -> kiem tra credentials Adafruit trong `.env`
