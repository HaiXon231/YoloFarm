# Plan 02-02: Security Fixes — Rate limit, MQTT thread-safety, Adafruit quota

## What was built
- Added `bucket4j-core` and implemented `RateLimitFilter` to rate-limit `/api/v1/auth/login` to 5 requests per minute per IP.
- Fixed `ConcurrentModificationException` risk in `MqttReceiverService` by replacing `ArrayList` with `CopyOnWriteArrayList` for MQTT observers.
- Handled cache eviction bug when renaming a device (Feed Key eviction) in `DeviceService`.
- Added quota limit protection for Adafruit free tier in `AdafruitApiServiceImpl` (throws clear `ConflictException` on HTTP 422 limit reached).

## Verification
- Code builds successfully.
- No concurrency warnings during MQTT callbacks.

## Next Steps
- Execute backend tests in Plan 02-03.
