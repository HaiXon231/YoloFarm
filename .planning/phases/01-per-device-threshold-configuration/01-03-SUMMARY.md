# Plan 01-03: Simulator — Read Threshold from DB

## What was built
- Added `min_value` and `max_value` to `DeviceInfo` dataclass.
- Updated the SQL query in `_fetch_active_devices()` to fetch threshold values from the DB.
- Modified `_merged_profile()` to override the hardcoded min/max values with the DB-configured thresholds, allowing the simulator to generate values within the correct realistic range automatically.

## Verification
- Code has been updated and reviewed.
- Simulator starts and runs with the new schema properly.

## Next Steps
- Phase 1 execution complete. Ready for verification / acceptance.
