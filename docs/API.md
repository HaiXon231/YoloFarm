# API Reference

Tai lieu nay tong hop nhanh cac endpoint chinh dua tren `backend/api/openAPI.yaml`.

## 1. Base

- Base URL: `http://localhost:8080`
- Prefix: `/api/v1`
- Auth mac dinh: `bearerAuth` (JWT)

## 2. Auth

| Method | Path | Mo ta |
|---|---|---|
| POST | `/api/v1/auth/register` | Dang ky tai khoan nong dan |
| POST | `/api/v1/auth/login` | Dang nhap, tra JWT |
| GET | `/api/v1/auth/me` | Lay profile hien tai |

## 3. Farms

| Method | Path | Mo ta |
|---|---|---|
| GET | `/api/v1/farms` | Danh sach farm cua user |
| POST | `/api/v1/farms` | Tao farm moi |
| GET | `/api/v1/farms/{farmId}` | Chi tiet farm |
| PUT | `/api/v1/farms/{farmId}` | Cap nhat farm |
| DELETE | `/api/v1/farms/{farmId}` | Xoa farm (neu khong con device/rule) |

## 4. Device Models

| Method | Path | Mo ta |
|---|---|---|
| GET | `/api/v1/device-models` | Danh sach model thiet bi |
| POST | `/api/v1/admin/device-models` | (ADMIN) Tao model moi |

## 5. Devices

| Method | Path | Mo ta |
|---|---|---|
| GET | `/api/v1/farms/{farmId}/devices` | Danh sach device trong farm |
| PATCH | `/api/v1/devices/{deviceId}` | Doi ten device |
| POST | `/api/v1/devices/requests` | (FARMER) Yeu cau them device |
| POST | `/api/v1/devices/{deviceId}/remove-requests` | (FARMER) Yeu cau go bo device |
| GET | `/api/v1/admin/devices/requests` | (ADMIN) Danh sach device cho duyet |
| POST | `/api/v1/admin/devices/{deviceId}/approve` | (ADMIN) Duyet + cap feed key |
| POST | `/api/v1/admin/devices/{deviceId}/reject` | (ADMIN) Tu choi yeu cau |
| DELETE | `/api/v1/admin/devices/{deviceId}` | (ADMIN) Xoa device va thu hoi feed key |
| PATCH | `/api/v1/devices/{deviceId}/mode` | Doi che do AUTO/MANUAL |

## 6. Rules

| Method | Path | Mo ta |
|---|---|---|
| GET | `/api/v1/farms/{farmId}/rules` | Danh sach rule theo farm |
| POST | `/api/v1/rules` | Tao rule moi |
| PATCH | `/api/v1/rules/{ruleId}/toggle` | Bat/tat rule |
| PUT | `/api/v1/rules/{ruleId}` | Cap nhat rule |
| DELETE | `/api/v1/rules/{ruleId}` | Xoa rule |

## 7. Telemetry

| Method | Path | Mo ta |
|---|---|---|
| GET | `/api/v1/devices/{deviceId}/telemetry` | Lay du lieu telemetry (query start/end, aggregate) |

## 8. Control

| Method | Path | Mo ta |
|---|---|---|
| POST | `/api/v1/devices/{deviceId}/command` | Gui lenh ON/OFF cho actuator |

## 9. AI Analysis

| Method | Path | Mo ta |
|---|---|---|
| POST | `/api/v1/farms/{farmId}/ai-analysis` | Upload anh, phan tich AI |
| GET | `/api/v1/farms/{farmId}/ai-logs` | Lich su phan tich AI |

## 10. Notifications

| Method | Path | Mo ta |
|---|---|---|
| GET | `/api/v1/notifications` | Danh sach thong bao |
| GET | `/api/v1/notifications/unread-count` | So luong thong bao chua doc |
| PUT | `/api/v1/notifications/{notificationId}/read` | Danh dau thong bao da doc |
| PUT | `/api/v1/notifications/read-all` | Danh dau tat ca thong bao da doc |

## 11. Chi tiet day du

Xem file: `backend/api/openAPI.yaml` de co schema, request/response va example chi tiet.
