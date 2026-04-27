# Plan 01-01: Backend Schema & API — Threshold Configuration

## What was built
- Added `min_value` and `max_value` to `Device` entity.
- Created `DeviceThresholdRequest` DTO.
- Added `updateThreshold` method in `DeviceService` and `PATCH /api/v1/devices/{deviceId}/threshold` endpoint in `DeviceController`.
- Implemented threshold validation in `MqttReceiverService` to log warnings when incoming telemetry falls outside the configured range.

## Verification
- Backend compiles successfully (`mvnw.cmd compile`).
- API conforms to required specifications.

## Next Steps
- Implement Frontend UI (Plan 01-02).
- Update Simulator (Plan 01-03).
