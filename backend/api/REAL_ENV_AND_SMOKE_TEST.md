# YoloFarm Real Config + Adafruit Smoke Test (Windows)

Tai lieu nay giup ban:
- Lay dung cac tham so cho `src/main/resources/application.yml`
- Cau hinh moi truong that de chay backend
- Test luong that end-to-end voi PostgreSQL + Adafruit MQTT

## 1) Cac bien cau hinh bat buoc

Ung dung dang doc cac bien sau:

- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET_KEY`
- `ADAFRUIT_USERNAME`
- `ADAFRUIT_IO_KEY`
- `ADAFRUIT_BROKER_URL` (co default `ssl://io.adafruit.com:8883`, co the bo qua)
- `WS_ALLOWED_ORIGINS` (co default `http://localhost:3000`)

## 2) Lay gia tri tung bien

### 2.1 DB_USERNAME, DB_PASSWORD

Nguon lay:
- Tai khoan PostgreSQL ban dang dung de dang nhap DB `yolofarm_db`
- Neu chua co DB, tao DB va user truoc khi chay backend

Kiem tra nhanh bang psql:

```powershell
psql -h localhost -U postgres -d yolofarm_db
```

Neu vao duoc DB, user/password hop le.

### 2.2 JWT_SECRET_KEY

Yeu cau:
- Chuoi bi mat, do dai manh (khuyen nghi >= 256-bit)
- Nen dung Base64 de de luu tru

Tao key moi bang PowerShell:

```powershell
$bytes = New-Object byte[] 64
[System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
[Convert]::ToBase64String($bytes)
```

Copy output va gan vao `JWT_SECRET_KEY`.

### 2.3 ADAFRUIT_USERNAME

Cach lay:
- Dang nhap https://io.adafruit.com/
- Vao profile/account, username chinh la `ADAFRUIT_USERNAME`

### 2.4 ADAFRUIT_IO_KEY

Cach lay:
- Dang nhap https://io.adafruit.com/
- Vao `My Key` -> `View AIO Key`
- Copy Active Key vao `ADAFRUIT_IO_KEY`

### 2.5 ADAFRUIT_BROKER_URL

Mac dinh he thong dang dung:
- `ssl://io.adafruit.com:8883`

Chi doi neu ban co nhu cau dac biet.

### 2.6 WS_ALLOWED_ORIGINS

Gan URL frontend that duoc phep ket noi websocket, vi du:
- `http://localhost:3000`
- Neu co nhieu origin: phan tach boi dau phay

## 3) Set bien moi truong tren Windows

### 3.1 Set tam thoi cho phien terminal hien tai

```powershell
$env:DB_USERNAME="<db_user>"
$env:DB_PASSWORD="<db_password>"
$env:JWT_SECRET_KEY="<base64_secret>"
$env:ADAFRUIT_USERNAME="<aio_username>"
$env:ADAFRUIT_IO_KEY="<aio_key>"
$env:ADAFRUIT_BROKER_URL="ssl://io.adafruit.com:8883"
$env:WS_ALLOWED_ORIGINS="http://localhost:3000"
```

### 3.2 Kiem tra da set du chua

```powershell
"DB_USERNAME","DB_PASSWORD","JWT_SECRET_KEY","ADAFRUIT_USERNAME","ADAFRUIT_IO_KEY" |
ForEach-Object {
  if ([string]::IsNullOrWhiteSpace((Get-Item "Env:$_" -ErrorAction SilentlyContinue).Value)) {
    Write-Host "MISSING: $_" -ForegroundColor Red
  } else {
    Write-Host "OK: $_" -ForegroundColor Green
  }
}
```

## 4) Chay backend voi cau hinh that

Tu thu muc `backend/api`:

```powershell
.\mvnw.cmd spring-boot:run
```

Dau hieu thanh cong:
- App boot khong loi
- Co log ket noi MQTT thanh cong
- Khong co loi `Could not resolve placeholder` cho cac bien env

## 5) Tao tai khoan FARMER va ADMIN de test luong that

### 5.1 Tao FARMER bang API

```powershell
$base = "http://localhost:8080"

$registerBody = @{
  username = "farmer_live"
  password = "Farmer@12345"
  email    = "farmer_live@example.com"
} | ConvertTo-Json

Invoke-RestMethod -Method Post -Uri "$base/api/v1/auth/register" -ContentType "application/json" -Body $registerBody
```

### 5.2 Dang nhap FARMER lay token

```powershell
$loginBody = @{
  username = "farmer_live"
  password = "Farmer@12345"
} | ConvertTo-Json

$farmerLogin = Invoke-RestMethod -Method Post -Uri "$base/api/v1/auth/login" -ContentType "application/json" -Body $loginBody
$farmerToken = $farmerLogin.access_token
$farmerHeaders = @{ Authorization = "Bearer $farmerToken" }
```

### 5.3 Tao ADMIN (tam thoi bang DB)

Hien tai register mac dinh tao role FARMER. Cach nhanh de test luong admin la update role trong DB:

```sql
update users set role = 'ADMIN' where username = 'admin_live';
```

Neu user `admin_live` chua ton tai, dang ky user moi roi update role thanh ADMIN.

Dang nhap admin lay token tuong tu nhu FARMER.

## 6) Smoke test luong that voi Adafruit

### Buoc A: FARMER tao farm

```powershell
$farmBody = @{ name = "Farm Live"; location = "Zone A" } | ConvertTo-Json
$farm = Invoke-RestMethod -Method Post -Uri "$base/api/v1/farms" -Headers $farmerHeaders -ContentType "application/json" -Body $farmBody
$farmId = $farm.id
```

### Buoc B: FARMER gui request tao device

Truoc do can co `model_id` hop le (tao boi admin hoac co san).

```powershell
$models = Invoke-RestMethod -Method Get -Uri "$base/api/v1/device-models" -Headers $farmerHeaders
$modelId = $models[0].id

$deviceReq = @{
  farm_id = $farmId
  model_id = $modelId
  name = "Pump Live"
} | ConvertTo-Json

$device = Invoke-RestMethod -Method Post -Uri "$base/api/v1/devices/requests" -Headers $farmerHeaders -ContentType "application/json" -Body $deviceReq
$deviceId = $device.id
```

### Buoc C: ADMIN duyet device va cap feed key

Feed key nen theo mau: `<username>/feeds/<feed_name>`

```powershell
$approveBody = @{ adafruit_feed_key = "$env:ADAFRUIT_USERNAME/feeds/pump-live" } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri "$base/api/v1/admin/devices/$deviceId/approve" -Headers $adminHeaders -ContentType "application/json" -Body $approveBody
```

### Buoc D: Ban telemetry that len Adafruit

Dung giao dien Adafruit IO de publish vao feed duoc cap, hoac dung MQTT client ben ngoai.

Topic publish:
- `$env:ADAFRUIT_USERNAME/feeds/<feed_name>`

Vi du feed name la `pump-live`, topic la:
- `your_username/feeds/pump-live`

### Buoc E: Kiem tra backend nhan va luu telemetry

```powershell
$start = [DateTimeOffset]::UtcNow.AddHours(-1).ToString("o")
$end = [DateTimeOffset]::UtcNow.ToString("o")
Invoke-RestMethod -Method Get -Uri "$base/api/v1/devices/$deviceId/telemetry?start_time=$start&end_time=$end" -Headers $farmerHeaders
```

### Buoc F: Test command that tu backend -> Adafruit

```powershell
$cmdBody = @{ command = "ON" } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri "$base/api/v1/devices/$deviceId/command" -Headers $farmerHeaders -ContentType "application/json" -Body $cmdBody
```

Kiem tra tren Adafruit feed tuong ung de thay lenh duoc day di.

## 7) Checklist hoan tat hom nay

- [ ] Backend boot thanh cong voi env that
- [ ] Dang ky/dang nhap FARMER thanh cong
- [ ] Tao FARM thanh cong
- [ ] Request + approve DEVICE thanh cong
- [ ] Telemetry tu Adafruit vao backend thanh cong
- [ ] Command tu backend ra Adafruit thanh cong
- [ ] API telemetry doc duoc data vua publish

---

Neu can, ban co the lam buoc smoke test bang Postman, chi can copy y nguyen endpoint/body nhu cac lenh PowerShell tren.
