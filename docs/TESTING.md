# Testing

## Backend

```powershell
cd backend/api
.\mvnw.cmd test
```

Chay mot test class:

```powershell
.\mvnw.cmd test -Dtest=RuleEngineObserverTest
```

## Frontend

Hien co Vitest config trong `frontend/vite.config.ts` va dependency `vitest`, nhung chua co script `test` trong `package.json`.

Co the chay truc tiep:

```powershell
cd frontend
npx vitest
```

Neu can, co the them script `"test": "vitest"` vao `frontend/package.json`.

## Simulator

Khong co test suite tu dong. Kiem tra nhanh:

```powershell
cd simulator/digital-twin
.\.venv\Scripts\python.exe -m py_compile main.py tools\feed_key_manager.py
```
