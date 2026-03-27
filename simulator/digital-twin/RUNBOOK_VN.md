# Huong Dan Chay Chi Tiet (It Roi Nhat)

Tai lieu nay giup ban chay simulator theo thu tu dung, co dau hieu de kiem tra tung buoc.

## 1) Muc tieu

- Backend giu nguyen, khong nhung code simulator.
- Simulator chay rieng tai thu muc `simulator/digital-twin`.
- Tu dong phat hien device ACTIVE va tao du lieu lien tuc.

## 2) Chuan bi truoc khi chay

1. Backend da chay va ket noi DB PostgreSQL duoc.
2. Trong DB da co mot vai device ACTIVE co `adafruit_feed_key`.
3. Co thong tin Adafruit:
   - `ADAFRUIT_USERNAME`
   - `ADAFRUIT_IO_KEY`

## 3) Cach chay de nhat (khuyen dung)

Tu thu muc goc du an `YoloFarm`:

```powershell
cd simulator/digital-twin
.\scripts\run-simulator.ps1
```

Script se tu dong:
1. Kiem tra Python
2. Tao `.venv` neu chua co
3. Cai dependencies
4. Tao `.env` va `profiles.json` tu file mau neu thieu
5. Kiem tra syntax script
6. Chay planner feed key (dry-run)
7. Chay simulator lien tuc

## 4) Lan dau can sua file nao

Sau lan chay dau, mo file `.env` va dien gia tri that:

- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_USER`
- `DB_PASSWORD`
- `ADAFRUIT_USERNAME`
- `ADAFRUIT_IO_KEY`

Neu chua muon chay simulator ngay, dung lenh:

```powershell
.\scripts\run-simulator.ps1 -InitOnly
```

## 5) Quan ly feed key (ngoai backend)

### 5.1 Xem truoc (khong ghi DB)

```powershell
.\scripts\run-simulator.ps1 -InitOnly
```

Hoac:

```powershell
.\.venv\Scripts\python.exe tools\feed_key_manager.py
```

### 5.2 Ghi feed key vao DB

```powershell
.\scripts\run-simulator.ps1 -ApplyFeedKeys -InitOnly
```

### 5.3 Rewrite toan bo feed key (can than)

```powershell
.\scripts\run-simulator.ps1 -ApplyFeedKeys -RewriteAllFeedKeys -InitOnly
```

## 6) Enforce khong trung feed key

Chay SQL sau tren PostgreSQL:

- `sql/001_unique_adafruit_feed_key.sql`

Tao du lieu mau device models (chay trong DBeaver):

- `../../backend/api/sql/seed_device_models.sql`

Co the chay bang pgAdmin hoac psql.

## 7) Dau hieu chay thanh cong

Khi simulator dang chay, se thay log dang:

- ket noi MQTT thanh cong
- runtime started cho tung device
- publish telemetry lien tuc len feed

Neu device la actuator, khi gui command `ON/OFF`, se thay log command duoc xu ly.

## 8) Kiem tra du lieu ve backend

1. Gui command tu backend (hoac Postman) toi device actuator.
2. Simulator nhan command qua feed va cap nhat state.
3. Backend receiver nhan telemetry va luu DB.
4. Goi API telemetry cua backend de xac nhan co du lieu moi.

## 9) Loi thuong gap

1. Bao loi thieu `.env`:
   - Chay lai script, no se tao tu `.env.example`.
   - Dien gia tri that trong `.env`.

2. MQTT khong ket noi:
   - Kiem tra `ADAFRUIT_USERNAME`/`ADAFRUIT_IO_KEY`.
   - Kiem tra internet va port 8883.

3. Khong co du lieu publish:
   - Kiem tra device co trang thai ACTIVE trong DB.
   - Kiem tra `adafruit_feed_key` khong rong.

4. Khong luu vao DB backend:
   - Kiem tra backend receiver dang subscribe feed.
   - Kiem tra feed key simulator trung feed key trong DB.

## 10) Tat tat ca luong local khi khong can nua

Tu thu muc goc `YoloFarm`:

```powershell
.\scripts\stop-all-local.ps1
```

Script nay se:
- Tat process dang chiem port 8080 (backend local)
- Tat python simulator dang chay `digital-twin/main.py`
