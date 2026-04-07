import json
import logging
import math
import os
import random
import select
import signal
import ssl
import threading
import time
from dataclasses import dataclass, field
from typing import Dict, Optional

import paho.mqtt.client as mqtt
import psycopg2
from dotenv import load_dotenv


@dataclass
class DeviceInfo:
    device_id: str
    feed_key: str
    device_type: str
    metric_type: str


@dataclass
class DeviceRuntime:
    info: DeviceInfo
    profile: dict
    stop_event: threading.Event = field(default_factory=threading.Event)
    thread: Optional[threading.Thread] = None
    current_value: float = 0.0
    actuator_state: int = 0


class DigitalTwinManager:
    def __init__(self):
        load_dotenv()

        self.logger = logging.getLogger("digital_twin")
        self.adafruit_username = os.getenv("ADAFRUIT_USERNAME", "").strip()
        self.adafruit_key = os.getenv("ADAFRUIT_IO_KEY", "").strip()
        self.adafruit_broker = os.getenv("ADAFRUIT_BROKER", "io.adafruit.com").strip()
        self.adafruit_port = int(os.getenv("ADAFRUIT_PORT", "8883"))
        self.sync_seconds = int(os.getenv("SYNC_SECONDS", "15"))  # Tăng từ 5s → 15s: giảm 66% DB query thừa
        self.profiles_path = os.getenv("SIM_PROFILES_FILE", "profiles.json")

        self._lock = threading.RLock()
        self._runtimes: Dict[str, DeviceRuntime] = {}
        self._running = True

        self.mqtt_client = mqtt.Client(client_id=f"yf-sim-{random.randint(1000, 9999)}")
        self.mqtt_client.username_pw_set(self.adafruit_username, self.adafruit_key)
        self.mqtt_client.tls_set(cert_reqs=ssl.CERT_REQUIRED)
        self.mqtt_client.on_connect = self._on_connect
        self.mqtt_client.on_message = self._on_message

    def _db_conn(self):
        return psycopg2.connect(
            host=os.getenv("DB_HOST", "localhost"),
            port=int(os.getenv("DB_PORT", "5432")),
            dbname=os.getenv("DB_NAME", "yolofarm_db"),
            user=os.getenv("DB_USER", "postgres"),
            password=os.getenv("DB_PASSWORD", ""),
        )

    def _load_profiles(self) -> dict:
        if not os.path.exists(self.profiles_path):
            return {}
        with open(self.profiles_path, "r", encoding="utf-8") as f:
            return json.load(f)

    def _merged_profile(self, info: DeviceInfo, profiles: dict) -> dict:
        defaults = profiles.get("defaults", {})
        metrics = profiles.get("metrics", {})
        devices = profiles.get("devices", {})

        metric_profile = metrics.get(info.metric_type.upper(), {})
        device_profile = devices.get(info.device_id, {})
        feed_profile = devices.get(f"feed:{info.feed_key}", {})

        merged = {}
        merged.update(defaults)
        merged.update(metric_profile)
        merged.update(device_profile)
        merged.update(feed_profile)

        if info.device_type.upper() == "ACTUATOR" and "pattern" not in merged:
            merged["pattern"] = "actuator_state"

        merged.setdefault("interval_ms", 2000)
        merged.setdefault("pattern", "random_walk")
        merged.setdefault("min", 0)
        merged.setdefault("max", 100)
        merged.setdefault("step", 1.0)
        merged.setdefault("noise", 0.2)

        return merged

    def _fetch_active_devices(self) -> Dict[str, DeviceInfo]:
        sql = """
        SELECT d.id::text, d.adafruit_feed_key, m.device_type::text, m.metric_type::text
        FROM devices d
        JOIN models m ON m.id = d.model_id
        WHERE d.status = 'ACTIVE'
          AND d.adafruit_feed_key IS NOT NULL
          AND trim(d.adafruit_feed_key) <> ''
        """

        devices = {}
        with self._db_conn() as conn:
            with conn.cursor() as cur:
                cur.execute(sql)
                for row in cur.fetchall():
                    info = DeviceInfo(
                        device_id=row[0],
                        feed_key=row[1].strip(),
                        device_type=row[2].upper(),
                        metric_type=row[3].upper(),
                    )
                    devices[info.device_id] = info
        return devices

    def _publish_value(self, runtime: DeviceRuntime, value: float):
        topic = f"{self.adafruit_username}/feeds/{runtime.info.feed_key}"
        payload = f"{value:.2f}" if isinstance(value, float) else str(value)
        self.mqtt_client.publish(topic, payload=payload, qos=1)
        self.logger.info("Publish telemetry device=%s feed=%s value=%s", runtime.info.device_id, runtime.info.feed_key, payload)

    def _compute_value(self, runtime: DeviceRuntime) -> float:
        profile = runtime.profile
        pattern = str(profile.get("pattern", "random_walk")).lower()
        vmin = float(profile.get("min", 0))
        vmax = float(profile.get("max", 100))
        step = float(profile.get("step", 1.0))
        noise = float(profile.get("noise", 0.2))

        if pattern == "actuator_state":
            return float(runtime.actuator_state)

        if pattern == "constant":
            base = float(profile.get("value", runtime.current_value or (vmin + vmax) / 2.0))
            return max(vmin, min(vmax, base + random.uniform(-noise, noise)))

        if pattern == "sine":
            period_seconds = float(profile.get("period_seconds", 60.0))
            t = time.time()
            mid = (vmax + vmin) / 2.0
            amp = (vmax - vmin) / 2.0
            val = mid + amp * math.sin((2.0 * math.pi / period_seconds) * t)
            return max(vmin, min(vmax, val + random.uniform(-noise, noise)))

        delta = random.uniform(-step, step)
        val = runtime.current_value + delta + random.uniform(-noise, noise)
        return max(vmin, min(vmax, val))

    def _device_loop(self, runtime: DeviceRuntime):
        interval = max(200, int(runtime.profile.get("interval_ms", 2000))) / 1000.0

        if runtime.current_value == 0.0:
            vmin = float(runtime.profile.get("min", 0))
            vmax = float(runtime.profile.get("max", 100))
            runtime.current_value = (vmin + vmax) / 2.0

        while not runtime.stop_event.is_set():
            value = self._compute_value(runtime)
            runtime.current_value = value
            self._publish_value(runtime, value)
            runtime.stop_event.wait(interval)

    def _start_runtime(self, info: DeviceInfo, profile: dict):
        runtime = DeviceRuntime(info=info, profile=profile)
        runtime.thread = threading.Thread(target=self._device_loop, args=(runtime,), daemon=True)

        with self._lock:
            self._runtimes[info.device_id] = runtime

        if info.device_type == "ACTUATOR":
            topic = f"{self.adafruit_username}/feeds/{info.feed_key}"
            self.mqtt_client.subscribe(topic, qos=1)
            self.logger.info("Subscribe actuator command topic=%s", topic)

        runtime.thread.start()
        self.logger.info("Runtime started device=%s feed=%s type=%s metric=%s", info.device_id, info.feed_key, info.device_type, info.metric_type)

    def _stop_runtime(self, device_id: str):
        with self._lock:
            runtime = self._runtimes.pop(device_id, None)

        if not runtime:
            return

        runtime.stop_event.set()
        if runtime.thread and runtime.thread.is_alive():
            runtime.thread.join(timeout=2)

        self.logger.info("Runtime stopped device=%s", device_id)

    def _sync_once(self):
        profiles = self._load_profiles()
        db_devices = self._fetch_active_devices()

        with self._lock:
            runtime_ids = set(self._runtimes.keys())

        db_ids = set(db_devices.keys())

        for missing_id in runtime_ids - db_ids:
            self._stop_runtime(missing_id)

        for device_id, info in db_devices.items():
            profile = self._merged_profile(info, profiles)
            with self._lock:
                existing = self._runtimes.get(device_id)

            if not existing:
                self._start_runtime(info, profile)
                continue

            changed = (
                existing.info.feed_key != info.feed_key
                or existing.info.device_type != info.device_type
                or existing.info.metric_type != info.metric_type
                or existing.profile != profile
            )
            if changed:
                self._stop_runtime(device_id)
                self._start_runtime(info, profile)

    def _on_connect(self, client, userdata, flags, rc):
        if rc == 0:
            self.logger.info("Connected to Adafruit MQTT broker")
        else:
            self.logger.error("MQTT connect failed rc=%s", rc)

    def _on_message(self, client, userdata, msg):
        payload = msg.payload.decode("utf-8", errors="ignore").strip().upper()
        parts = msg.topic.split("/")
        if len(parts) < 3:
            return

        feed_key = parts[-1]
        with self._lock:
            runtimes = list(self._runtimes.values())

        for runtime in runtimes:
            if runtime.info.feed_key != feed_key:
                continue
            if runtime.info.device_type != "ACTUATOR":
                continue

            if payload in ("ON", "1"):
                runtime.actuator_state = 1
            elif payload in ("OFF", "0"):
                runtime.actuator_state = 0
            elif payload.startswith("SET:"):
                try:
                    runtime.current_value = float(payload.split(":", 1)[1])
                except ValueError:
                    pass

            self.logger.info("Actuator command device=%s feed=%s command=%s state=%s", runtime.info.device_id, feed_key, payload, runtime.actuator_state)
            return

    def run(self):
        self.mqtt_client.connect(self.adafruit_broker, self.adafruit_port, keepalive=60)
        self.mqtt_client.loop_start()

        self.logger.info("Digital twin runtime started")

        # Đồng bộ hóa lần đầu khi khởi động
        self._sync_once()

        db_conn = None
        while self._running:
            try:
                # Nếu mất kết nối, khởi tạo lại
                if not db_conn or db_conn.closed:
                    self.logger.info("Connecting to PostgreSQL for LISTEN...")
                    db_conn = self._db_conn()
                    db_conn.set_isolation_level(psycopg2.extensions.ISOLATION_LEVEL_AUTOCOMMIT)
                    cur = db_conn.cursor()
                    cur.execute("LISTEN device_events;")
                    self.logger.info("Listening for PostgreSQL NOTIFY on 'device_events'...")

                # Ngủ đông ở mức OS (tối đa 5s để còn check lệnh tắt máy tự động)
                # Sẽ bật dậy LUÔN VÀ NGAY khi có thông báo từ DB
                if select.select([db_conn], [], [], 5) == ([], [], []):
                    pass # Chỉ là timeout ngủ đông thôi
                else:
                    db_conn.poll()
                    while db_conn.notifies:
                        notify = db_conn.notifies.pop(0)
                        self.logger.info("Thức giấc bởi NOTIFY: kênh=%s, tín_hiệu=%s", notify.channel, notify.payload)
                        self._sync_once()

            except Exception as ex:
                self.logger.exception("Lỗi ở vòng lặp Lắng nghe (LISTEN): %s", ex)
                if db_conn and not db_conn.closed:
                    db_conn.close()
                db_conn = None
                time.sleep(5)

        if db_conn and not db_conn.closed:
            db_conn.close()

        self.logger.info("Stopping runtimes")
        with self._lock:
            device_ids = list(self._runtimes.keys())
        for device_id in device_ids:
            self._stop_runtime(device_id)

        self.mqtt_client.loop_stop()
        self.mqtt_client.disconnect()

    def stop(self):
        self._running = False


def main():
    load_dotenv()

    log_level = os.getenv("LOG_LEVEL", "INFO").upper()
    logging.basicConfig(
        level=getattr(logging, log_level, logging.INFO),
        format="%(asctime)s %(levelname)s [%(name)s] %(message)s",
    )

    missing = []
    for key in ["DB_PASSWORD", "ADAFRUIT_USERNAME", "ADAFRUIT_IO_KEY"]:
        if not os.getenv(key):
            missing.append(key)

    if missing:
        raise RuntimeError(f"Missing required env vars: {', '.join(missing)}")

    manager = DigitalTwinManager()

    def _handle_stop(signum, frame):
        manager.stop()

    signal.signal(signal.SIGINT, _handle_stop)
    signal.signal(signal.SIGTERM, _handle_stop)

    manager.run()


if __name__ == "__main__":
    main()
