# Testing

## Backend

Command:

```bash
cd backend
./mvnw test
```

Backend tests should cover:

- Pure domain logic with unit tests.
- Services with mocked dependencies when behavior is isolated.
- Controllers with Spring MVC/security test support.
- Integration-style flows for notification connection, reminder processing, and retry when persistence matters.

Important areas:

- Auth and user scoping.
- Cycle prediction.
- Settings validation.
- Telegram connect/disconnect.
- Discord connect/callback/disconnect.
- Reminder cron processing.
- Failed notification retry.
- Duplicate prevention for already-sent reminders.

## Frontend Unit Tests

Command:

```bash
cd frontend
yarn test
```

Use Vitest and Testing Library.

Good UT targets:

- API client behavior.
- API wrapper functions.
- Component actions that call APIs and show toasts.
- Error handling and unauthorized behavior.
- Small UI state transitions that do not need a real browser.

Avoid making component UTs duplicate full browser flows. Put full user flows in Playwright.

## Frontend E2E Tests

Command:

```bash
cd frontend
yarn test:e2e
```

Playwright tests live in `frontend/e2e`.

Default approach:

- Mock backend API routes in Playwright unless the task explicitly asks for full-stack E2E.
- Seed auth through localStorage when testing authenticated screens.
- Use role/text selectors where practical.
- Assert user-visible behavior, not implementation details.

Good E2E targets:

- Login/register redirect.
- Onboarding happy path.
- Settings save.
- Telegram test notification and disconnect.
- Discord callback result handling.
- Notification history rendering.

## Lint and Build

Frontend lint:

```bash
cd frontend
yarn lint
```

Frontend build:

```bash
cd frontend
yarn build
```

Run build when changing Next config, app layout, routing, server/client boundaries, or dependency setup.

## Test Selection

- Backend-only change: backend tests.
- Frontend component/API change: frontend lint and unit tests.
- Frontend user-flow change: frontend lint, unit tests if logic changed, and Playwright E2E.
- Contract change across backend/frontend: backend tests, frontend lint, frontend unit tests, and relevant E2E.

## Playwright Notes

Playwright starts the frontend on port `3100` by default.

Install Chromium if missing:

```bash
cd frontend
yarn playwright install chromium
```

If local port binding fails under a sandboxed runner, rerun with permission to bind a local port.
