# Plan 02-01: Flyway Migration — Baseline V1

## What was built
- Added `flyway-core` and `flyway-database-postgresql` to `pom.xml`.
- Created a Flyway baseline script `V1__init_schema.sql` matching the current Hibernate entities.
- Replaced `ddl-auto: update` with `ddl-auto: validate` in `application.yml` and enabled Flyway with `baseline-on-migrate: true`.

## Verification
- Backend compiles and boots successfully.
- Flyway executes schema generation without errors.

## Next Steps
- Execute security enhancements in Plan 02-02.
