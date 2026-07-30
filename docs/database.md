# Database

## Database Engine

Bebo uses PostgreSQL. Local development uses the `postgres` service in `compose.yaml`.

Default local connection:

```text
jdbc:postgresql://localhost:5433/bebo
username: bebo
password: bebo
```

## Migration Ownership

Flyway owns schema changes.

- Migration path: `backend/src/main/resources/db/migration`
- Hibernate setting: `ddl-auto: validate`
- Do not depend on Hibernate to create or alter production tables.

## Existing Migrations

- `V1__create_initial_schema.sql`: initial users, settings, cycle, and auth-related schema.
- `V2__telegram_connection.sql`: Telegram notification channel support.
- `V3__allow_pending_notification_channel.sql`: pending notification channel state.
- `V4__add_notification_retry.sql`: retry metadata for notification logs.
- `V5__add_user_onboarding_state.sql`: user onboarding state.
- `V6__add_daily_cycle_reminders.sql`: daily reminder support.
- `V7__add_discord_notification_channel.sql`: Discord channel support.
- `V8__rename_onboarding_channel_step.sql`: onboarding step rename.

## Migration Guidelines

- Add a new `V<number>__description.sql` file for every schema change.
- Never edit already-applied migrations unless the database has not been shared and the user explicitly asks.
- Keep migrations deterministic and reversible by inspection.
- Prefer explicit constraints for ownership, uniqueness, and status values.
- Keep enum-like values aligned with Java enums and frontend TypeScript unions.

## Important Data Concepts

- User: account, status, timezone, onboarding state.
- Cycle settings: default length, reminder days, notification time, timezone.
- Cycle record: user-entered cycle start date.
- Notification channel: Telegram/Discord channel identity and status.
- Notification log: delivery attempts, status, sent time, retry time, failure details.

## Data Integrity Rules

- User-owned data must be scoped by authenticated user.
- External chat/account identifiers should not be linked to multiple active users.
- Sent notifications should remain queryable for history.
- Failed notification logs should preserve enough data to retry safely.
- Date/time fields should make timezone assumptions explicit in service code.

## When Changing Entities

1. Update the Java entity.
2. Add a Flyway migration.
3. Update repository queries if needed.
4. Update DTOs and frontend types if API output changes.
5. Add or update tests.
6. Run `cd backend && ./mvnw test`.
