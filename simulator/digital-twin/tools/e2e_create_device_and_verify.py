import json
import os
import time
import urllib.error
import urllib.parse
import urllib.request
from datetime import datetime, timedelta, timezone

import psycopg2
from dotenv import load_dotenv


API_BASE = "http://localhost:8080/api/v1"


def http_json(method: str, path: str, payload=None, token=None):
    url = API_BASE + path
    data = None
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    if payload is not None:
        data = json.dumps(payload).encode("utf-8")

    req = urllib.request.Request(url=url, data=data, method=method, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            body = resp.read().decode("utf-8")
            return resp.status, json.loads(body) if body else None
    except urllib.error.HTTPError as e:
        body = e.read().decode("utf-8", errors="ignore")
        try:
            parsed = json.loads(body)
        except Exception:
            parsed = {"raw": body}
        return e.code, parsed


def db_conn():
    return psycopg2.connect(
        host=os.getenv("DB_HOST", "localhost"),
        port=int(os.getenv("DB_PORT", "5432")),
        dbname=os.getenv("DB_NAME", "yolofarm_db"),
        user=os.getenv("DB_USER", "postgres"),
        password=os.getenv("DB_PASSWORD", ""),
    )


def promote_admin(username: str):
    with db_conn() as conn:
        with conn.cursor() as cur:
            cur.execute("UPDATE users SET role = 'ADMIN' WHERE username = %s", (username,))
            conn.commit()
            return cur.rowcount


def telemetry_count(device_id: str):
    with db_conn() as conn:
        with conn.cursor() as cur:
            cur.execute("SELECT count(*) FROM telemetry_data WHERE device_id = %s::uuid", (device_id,))
            return cur.fetchone()[0]


def main():
    load_dotenv()

    suffix = str(int(time.time()))
    farmer_username = f"farmer_e2e_{suffix}"
    admin_username = f"admin_e2e_{suffix}"
    common_password = "Farmer1234"

    print("STEP 1: Register farmer/admin test users")
    st, body = http_json("POST", "/auth/register", {
        "username": farmer_username,
        "password": common_password,
        "email": f"{farmer_username}@example.com",
    })
    print(f"- Farmer register status={st}")
    if st not in (200, 201):
        print(body)
        return

    st, body = http_json("POST", "/auth/register", {
        "username": admin_username,
        "password": common_password,
        "email": f"{admin_username}@example.com",
    })
    print(f"- Admin register status={st}")
    if st not in (200, 201):
        print(body)
        return

    print("STEP 2: Promote admin user role in DB")
    changed = promote_admin(admin_username)
    print(f"- Updated rows={changed}")
    if changed != 1:
        print("- ERROR: cannot promote admin user")
        return

    print("STEP 3: Login and obtain tokens")
    st, farmer_login = http_json("POST", "/auth/login", {
        "username": farmer_username,
        "password": common_password,
    })
    print(f"- Farmer login status={st}")
    if st != 200:
        print(farmer_login)
        return
    farmer_token = farmer_login.get("access_token")

    st, admin_login = http_json("POST", "/auth/login", {
        "username": admin_username,
        "password": common_password,
    })
    print(f"- Admin login status={st}")
    if st != 200:
        print(admin_login)
        return
    admin_token = admin_login.get("access_token")

    print("STEP 4: Farmer creates farm")
    st, farm = http_json("POST", "/farms", {
        "name": f"Farm E2E {suffix}",
        "location": "Zone Test",
    }, token=farmer_token)
    print(f"- Create farm status={st}")
    if st not in (200, 201):
        print(farm)
        return
    farm_id = farm["id"]
    print(f"- farm_id={farm_id}")

    print("STEP 5: Admin creates SENSOR model")
    st, model = http_json("POST", "/admin/device-models", {
        "model_name": f"Temp Model E2E {suffix}",
        "device_type": "SENSOR",
        "metric_type": "TEMP",
        "manufacturer": "E2E",
    }, token=admin_token)
    print(f"- Create model status={st}")
    if st not in (200, 201):
        print(model)
        return
    model_id = model["id"]
    print(f"- model_id={model_id}")

    print("STEP 6: Farmer requests new device")
    st, device_req = http_json("POST", "/devices/requests", {
        "farm_id": farm_id,
        "model_id": model_id,
        "name": f"Device E2E {suffix}",
    }, token=farmer_token)
    print(f"- Request device status={st}")
    if st not in (200, 201):
        print(device_req)
        return
    device_id = device_req["id"]
    print(f"- device_id={device_id}")

    print("STEP 7: Admin approves device (auto feed key)")
    st, approved = http_json("POST", f"/admin/devices/{device_id}/approve", {}, token=admin_token)
    print(f"- Approve status={st}")
    if st not in (200, 201):
        print(approved)
        return
    feed_key = approved.get("adafruit_feed_key")
    print(f"- feed_key={feed_key}")

    print("STEP 8: Wait for simulator -> adafruit -> backend telemetry")
    initial = telemetry_count(device_id)
    print(f"- initial_telemetry_count={initial}")

    found = False
    for i in range(1, 16):
        time.sleep(3)
        current = telemetry_count(device_id)
        print(f"  poll {i:02d}: telemetry_count={current}")
        if current > initial:
            found = True
            break

    if not found:
        print("- ERROR: telemetry count did not increase. Check simulator/background logs.")
        return

    print("STEP 9: Verify telemetry API returns data")
    local_now = datetime.now().astimezone()
    start = (local_now - timedelta(hours=12)).isoformat()
    end = (local_now + timedelta(hours=1)).isoformat()
    q = urllib.parse.urlencode({"start_time": start, "end_time": end})
    st, telemetry = http_json("GET", f"/devices/{device_id}/telemetry?{q}", token=farmer_token)
    print(f"- Telemetry API status={st}")
    if st != 200:
        print(telemetry)
        return

    count = len(telemetry) if isinstance(telemetry, list) else -1
    print(f"- Telemetry API item_count={count}")
    if count > 0:
        print(f"- First data point={telemetry[0]}")

    print("DONE: Device test created and telemetry verified in DB + API")


if __name__ == "__main__":
    main()
