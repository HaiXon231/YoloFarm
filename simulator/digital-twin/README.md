# YoloFarm Digital Twin Simulator

A standalone simulator service that is fully separated from backend code.

It continuously scans backend PostgreSQL for ACTIVE devices with `adafruit_feed_key`, then auto-creates runtime channels in memory:
- SENSOR: publishes telemetry continuously to Adafruit feed.
- ACTUATOR: subscribes to command feed and publishes state telemetry continuously.

No backend code changes are required for simulation.

## 1. What this solves

- Auto-start simulation when a device becomes ACTIVE.
- Stop simulation when device is no longer ACTIVE.
- Continuous data generation based on configurable profiles.
- Command handling for actuators (`ON`, `OFF`, `SET:<value>`).
- Centralized control via profile file (interval, pattern, min/max/noise).

## 2. Prerequisites

- Python 3.10+
- Access to backend PostgreSQL database
- Adafruit username and AIO key

## 3. Setup

```powershell
cd simulator/digital-twin
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
copy .env.example .env
copy profiles.example.json profiles.json
```

Edit `.env` and set real values:
- `DB_*` to backend DB
- `ADAFRUIT_USERNAME`
- `ADAFRUIT_IO_KEY`

Quick path for less manual setup:

```powershell
.\scripts\run-simulator.ps1
```

Detailed Vietnamese runbook:
- `RUNBOOK_VN.md`

## 4. Run

```powershell
python main.py
```

Or one-click runner (recommended):

```powershell
.\scripts\run-simulator.ps1
```

The runtime loop behavior:
1. Every `SYNC_SECONDS`, query DB for ACTIVE devices.
2. Start simulator runtime for new devices.
3. Reload runtime when feed/profile changed.
4. Stop runtime when device is removed/deactivated.

## 5. Profile control

`profiles.json` has 3 levels:
- `defaults`: global defaults for all devices.
- `metrics`: per metric type (`TEMP`, `SOIL_MOISTURE`, `PUMP`, ...).
- `devices`: per specific device override by device id or feed key.

Override by feed key example:

```json
{
  "devices": {
    "feed:demo-pump-01": {
      "pattern": "actuator_state",
      "interval_ms": 1000
    }
  }
}
```

## 6. Patterns

Supported patterns:
- `random_walk`
- `constant`
- `sine`
- `actuator_state`

Useful parameters:
- `interval_ms`
- `min`
- `max`
- `step`
- `noise`
- `period_seconds` (for sine)

## 7. Feed key uniqueness note

To avoid collisions when users have multiple devices with the same model, feed keys must be unique per device (not per model).

Recommended format:
- `yf.u<owner8>.d<device12>.telemetry`
- or current backend format with device segment included.

As long as device id segment is part of feed key, duplicates are prevented.

## 8. Command simulation for actuator

This service listens to the actuator feed topic and applies commands:
- `ON` or `1` => state = 1
- `OFF` or `0` => state = 0
- `SET:<number>` => set current value directly

It then keeps publishing state telemetry continuously so backend can store/update state history.

## 9. Separation boundary

This simulator is outside backend and can be deployed/stopped independently.
Backend remains a pure API + business service.

## 10. Feed key governance (outside backend)

This project keeps feed-key governance external too:

- SQL hardening script: `sql/001_unique_adafruit_feed_key.sql`
- Feed key generator/manager: `tools/feed_key_manager.py`

### 10.1 Generate missing feed keys (dry run)

```powershell
python tools/feed_key_manager.py
```

### 10.2 Apply generated keys to DB

```powershell
python tools/feed_key_manager.py --apply
```

### 10.3 Rewrite all keys (optional)

```powershell
python tools/feed_key_manager.py --rewrite-all --apply
```

### 10.4 Enforce uniqueness in DB

Run SQL file `sql/001_unique_adafruit_feed_key.sql` on PostgreSQL.

It enforces a case-insensitive unique index on non-null keys so duplicate feed keys cannot be inserted.
