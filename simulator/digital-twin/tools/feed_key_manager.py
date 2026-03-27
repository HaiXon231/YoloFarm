import argparse
import os
from dataclasses import dataclass

import psycopg2
from dotenv import load_dotenv


@dataclass
class DeviceRow:
    device_id: str
    owner_id: str
    metric_type: str
    feed_key: str | None


def db_conn():
    return psycopg2.connect(
        host=os.getenv("DB_HOST", "localhost"),
        port=int(os.getenv("DB_PORT", "5432")),
        dbname=os.getenv("DB_NAME", "yolofarm_db"),
        user=os.getenv("DB_USER", "postgres"),
        password=os.getenv("DB_PASSWORD", ""),
    )


def short_id(raw: str, length: int) -> str:
    return raw.replace("-", "")[:length]


def normalized_key(value: str | None) -> str | None:
    if value is None:
        return None
    trimmed = value.strip().lower()
    return trimmed if trimmed else None


def generate_base_key(owner_id: str, device_id: str, metric_type: str) -> str:
    return f"yf.u{short_id(owner_id, 8)}.d{short_id(device_id, 12)}.{metric_type.lower()}"


def fetch_devices(conn, rewrite_all: bool) -> list[DeviceRow]:
    sql = """
        SELECT d.id::text, f.owner_id::text, m.metric_type::text, d.adafruit_feed_key
        FROM devices d
        JOIN farms f ON f.id = d.farm_id
        JOIN models m ON m.id = d.model_id
    """
    if not rewrite_all:
        sql += " WHERE d.adafruit_feed_key IS NULL OR btrim(d.adafruit_feed_key) = ''"

    with conn.cursor() as cur:
        cur.execute(sql)
        rows = cur.fetchall()
    return [DeviceRow(*r) for r in rows]


def existing_keys(conn) -> set[str]:
    sql = """
        SELECT lower(btrim(adafruit_feed_key))
        FROM devices
        WHERE adafruit_feed_key IS NOT NULL
          AND btrim(adafruit_feed_key) <> ''
    """
    with conn.cursor() as cur:
        cur.execute(sql)
        return {r[0] for r in cur.fetchall()}


def allocate_unique_key(base_key: str, used: set[str]) -> str:
    candidate = base_key
    suffix = 1
    while normalized_key(candidate) in used:
        suffix += 1
        candidate = f"{base_key}-{suffix}"
    used.add(normalized_key(candidate))
    return candidate


def apply_updates(conn, updates: list[tuple[str, str]], dry_run: bool):
    if dry_run:
        return

    with conn.cursor() as cur:
        for device_id, feed_key in updates:
            cur.execute(
                "UPDATE devices SET adafruit_feed_key = %s WHERE id = %s::uuid",
                (feed_key, device_id),
            )
    conn.commit()


def main():
    load_dotenv()

    parser = argparse.ArgumentParser(description="Generate/manage unique Adafruit feed keys outside backend")
    parser.add_argument("--apply", action="store_true", help="Apply updates to database")
    parser.add_argument("--rewrite-all", action="store_true", help="Rewrite all device feed keys")
    args = parser.parse_args()

    dry_run = not args.apply

    with db_conn() as conn:
        used = existing_keys(conn)
        devices = fetch_devices(conn, rewrite_all=args.rewrite_all)

        updates: list[tuple[str, str]] = []
        for row in devices:
            base = generate_base_key(row.owner_id, row.device_id, row.metric_type)
            key = allocate_unique_key(base, used)
            updates.append((row.device_id, key))

        if not updates:
            print("No devices need feed key updates.")
            return

        print(f"Planned updates: {len(updates)}")
        for device_id, feed_key in updates:
            print(f"- {device_id} -> {feed_key}")

        apply_updates(conn, updates, dry_run=dry_run)

        if dry_run:
            print("Dry run complete. Use --apply to persist changes.")
        else:
            print("Feed keys updated successfully.")


if __name__ == "__main__":
    main()
