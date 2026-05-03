# Deployment

Tai lieu nay tong hop cac file va luong deploy hien co trong repo.

## 1. Backend (Spring Boot)

### Dockerfile

- `backend/api/Dockerfile`
- Multi-stage build (Maven -> JRE)
- Expose port `7860` va set `SERVER_PORT=7860` (phu hop HuggingFace Spaces)

### Build va run (Docker)

```bash
docker build -t yolofarm-backend -f backend/api/Dockerfile backend/api

docker run -p 7860:7860 \
  -e DB_USERNAME=... \
  -e DB_PASSWORD=... \
  -e JWT_SECRET_KEY=... \
  -e ADAFRUIT_USERNAME=... \
  -e ADAFRUIT_IO_KEY=... \
  yolofarm-backend
```

### Bien moi truong bat buoc

- `DB_USERNAME`, `DB_PASSWORD`
- `JWT_SECRET_KEY`
- `ADAFRUIT_USERNAME`, `ADAFRUIT_IO_KEY`
- Optional: `ADAFRUIT_BROKER_URL`, `WS_ALLOWED_ORIGINS`

## 2. Frontend (React + Vite)

### Vercel

- File: `frontend/vercel.json`
- Rewrite tat ca route ve `index.html` (SPA)

### Build

```bash
cd frontend
npm install
npm run build
```

Upload thu muc `dist/` len host tich hop (Vercel/Netlify/Static host).

## 3. Simulator (Python)

Simulator chay nhu mot service doc lap, can DB + Adafruit credentials.

```bash
cd simulator/digital-twin
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
python main.py
```

## 4. Luu y khi deploy

- Backend va simulator can truy cap chung DB PostgreSQL.
- Frontend can biet base API (co the proxy hoac set `VITE_API_URL`).
- Mo cong WebSocket `/ws` va HTTP `/api/v1`.

## 5. Tinh trang hien tai

Repo chua co pipeline CI/CD tu dong (GitHub Actions, Docker Compose, v.v.).
Neu muon, co the them theo huong:

- Docker Compose cho backend + DB + simulator
- CI build/test cho backend va frontend
