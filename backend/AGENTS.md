# Backend Guide for Codex

This directory contains the Spring Boot backend for Bebo.

## Stack

- Java 21.
- Spring Boot 4.1.
- Spring MVC.
- Spring Security and JWT.
- Spring Data JPA.
- PostgreSQL.
- Flyway.
- Maven wrapper.

## Commands

Run from `backend/`:

- Dev server: `./mvnw spring-boot:run`
- Tests: `./mvnw test`

Local database is expected on `localhost:5433` by default. Start it from repo root with:

```bash
docker compose up -d postgres
```

## Code Organization

- `auth`: login/register and token issuing.
- `user`: user profile and onboarding.
- `settings`: cycle/reminder settings.
- `cycle`: cycle records and prediction.
- `notification`: shared channel/log/history model.
- `notification/telegram`: Telegram connect, polling, and sending.
- `notification/discord`: Discord OAuth, connect, and sending.
- `notification/reminder`: reminder plans, cron processing, and retry.
- `security`: JWT, CORS, and current user resolution.
- `common`: shared errors and small common controllers.

## Change Guidelines

- Put business decisions in services, not controllers.
- Keep controllers thin and explicit about HTTP status codes.
- Scope all user data by authenticated user.
- Use Flyway migrations for schema changes.
- Keep entity changes, DTO changes, migrations, and frontend type changes aligned.
- Use `Clock` injection where time-sensitive logic needs deterministic tests.
- Keep scheduled jobs idempotent.
- Do not log secrets, JWTs, bot tokens, OAuth codes, Telegram start tokens, or raw authorization headers.

## Notification Rules

- Telegram and Discord can be disabled by env.
- Disabled providers should fail clearly without creating misleading connected state.
- Disconnect endpoints should return `204 No Content`.
- Notification delivery should write/update `NotificationLog` so history and retry behavior stay auditable.
- Retry processors should only retry eligible failed logs.
- Sent notifications should not be resent for the same intended reminder occurrence.

## Testing Expectations

Run `./mvnw test` after backend changes.

Add/update tests when changing:

- Service behavior.
- Controller responses/status codes.
- Security/auth behavior.
- Flyway-backed persistence behavior.
- Reminder or retry processors.
- Telegram/Discord connection and delivery logic.

Prefer focused tests near the changed module. Add integration tests when repository/database behavior matters.
