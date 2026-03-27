-- Run on PostgreSQL used by backend before enforcing unique feed keys.
-- This script is external to backend code by design.

-- 1) Normalize blank values to NULL.
UPDATE devices
SET adafruit_feed_key = NULL
WHERE adafruit_feed_key IS NOT NULL
  AND btrim(adafruit_feed_key) = '';

-- 2) Check duplicates first (must return zero rows).
-- SELECT lower(btrim(adafruit_feed_key)) AS normalized_key, COUNT(*)
-- FROM devices
-- WHERE adafruit_feed_key IS NOT NULL
-- GROUP BY lower(btrim(adafruit_feed_key))
-- HAVING COUNT(*) > 1;

-- 3) Enforce case-insensitive uniqueness for non-null feed keys.
CREATE UNIQUE INDEX IF NOT EXISTS ux_devices_adafruit_feed_key
ON devices (lower(btrim(adafruit_feed_key)))
WHERE adafruit_feed_key IS NOT NULL;
