# Bebo Project Guide for Codex

## Project Overview

Bebo is a cycle tracking and reminder app with:

- `backend/`: Spring Boot backend, Java 21, PostgreSQL, Flyway, Spring Security JWT.
- `frontend/`: Next.js app, React 19, TypeScript, Tailwind CSS, next-intl, Zustand, Sonner toasts.
- `compose.yaml`: local PostgreSQL service on host port `5433` by default.

The backend owns business rules, persistence, auth, notification channels, reminder scheduling, delivery retry, and public callbacks/webhooks.
The frontend owns user flows for auth, onboarding, dashboard, settings, notification history, Telegram/Discord connect/disconnect, and toast feedback.

## Start Here

- Read `README.md` for local setup and common workflows.
- Read `docs/architecture.md` before changing cross-cutting backend/frontend behavior.
- Read `docs/domain-rules.md` before changing cycle prediction, reminders, notification delivery, Telegram, or Discord.
- Read `docs/database.md` before changing entities, repositories, or Flyway migrations.
- Read `docs/testing.md` before adding or updating tests.
- Read `.agent/PLANS.md` when working on multi-step changes that should be tracked across turns.
- Also read the nearest nested `AGENTS.md` when working under `backend/` or `frontend/`.

## Repository Layout

- `backend/src/main/java/com/bebo/auth`: login/register/auth APIs.
- `backend/src/main/java/com/bebo/user`: user profile and onboarding state.
- `backend/src/main/java/com/bebo/settings`: cycle and reminder settings.
- `backend/src/main/java/com/bebo/cycle`: cycle records and prediction logic.
- `backend/src/main/java/com/bebo/notification`: notification channels, logs, history, dispatching.
- `backend/src/main/java/com/bebo/notification/telegram`: Telegram connection and delivery.
- `backend/src/main/java/com/bebo/notification/discord`: Discord connection and delivery.
- `backend/src/main/java/com/bebo/notification/reminder`: reminder planning, cron execution, retry.
- `backend/src/main/resources/db/migration`: Flyway migrations.
- `frontend/src/app`: Next app routes.
- `frontend/src/features`: feature components.
- `frontend/src/lib/api`: typed frontend API clients.
- `frontend/src/stores`: Zustand stores.
- `frontend/messages`: English and Vietnamese translations.
- `frontend/e2e`: Playwright E2E tests.

## Local Environment

Backend config imports `../.env` optionally. Local defaults are useful for development:

- Backend URL: `http://localhost:8080`
- Frontend URL: `http://localhost:3000`
- PostgreSQL JDBC URL: `jdbc:postgresql://localhost:5433/bebo`
- PostgreSQL user/password/db: `bebo` / `bebo` / `bebo`
- Frontend API env: `NEXT_PUBLIC_API_URL=http://localhost:8080`

Telegram and Discord integrations are disabled unless env enables them:

- `TELEGRAM_BOT_ENABLED=true`
- `TELEGRAM_BOT_TOKEN`
- `TELEGRAM_BOT_USERNAME`
- `DISCORD_BOT_ENABLED=true`
- `DISCORD_CLIENT_ID`
- `DISCORD_CLIENT_SECRET`
- `DISCORD_BOT_TOKEN`

Never commit real secrets.

## Common Commands

From repo root:

- Start database: `docker compose up -d postgres`
- Backend dev server: `cd backend && ./mvnw spring-boot:run`
- Backend tests: `cd backend && ./mvnw test`
- Frontend dev server: `cd frontend && yarn dev`
- Frontend lint: `cd frontend && yarn lint`
- Frontend unit tests: `cd frontend && yarn test`
- Frontend E2E tests: `cd frontend && yarn test:e2e`
- Frontend build: `cd frontend && yarn build`

Playwright E2E uses its own Next dev server on port `3100` by default.
If Playwright browsers are missing, run `cd frontend && yarn playwright install chromium`.

## Backend Guidelines

- Keep business logic in services; controllers should validate/authenticate, call services, and map DTOs.
- Use repositories for persistence access, not ad hoc entity manager work unless needed.
- Use Flyway for schema changes. Do not rely on Hibernate auto-DDL; `ddl-auto` is `validate`.
- Keep date/time logic explicit. Store and compare instants in UTC where possible; use user time zones only for scheduling/display decisions.
- Preserve idempotency in notification/reminder jobs. A sent reminder should not be sent again for the same intended occurrence.
- For Telegram/Discord connection flows, keep these states clear: `DISCONNECTED`, `PENDING`, `CONNECTED`, `ALREADY_LINKED`.
- For disconnect endpoints, return `204 No Content` and ensure frontend API client handles empty bodies.
- Add focused tests for service behavior, controller behavior, cron processors, and retry rules when changing notification logic.
- Prefer structured logging around connection attempts, webhook/callback handling, reminder sending, and retry decisions. Do not log tokens, secrets, or full authorization payloads.

## Frontend Guidelines

- This app uses a modern Next.js version. When changing Next-specific APIs or app structure, check the local Next docs under `frontend/node_modules/next/dist/docs/` if available.
- Use existing app patterns: client components for interactive flows, `src/lib/api/*` for API calls, `src/types/*` for shared shapes, Zustand for auth state.
- Keep user-facing copy in `frontend/messages/en.json` and `frontend/messages/vi.json`.
- Use Sonner for success/info/error toasts. Toaster is globally mounted and should stay bottom-right.
- When an action has a toast success, avoid duplicating the same success message as inline UI. Inline errors are fine when they help recovery.
- Buttons and clickable controls should clearly feel interactive, including pointer cursor and disabled states.
- Prefer lucide icons for buttons and compact controls when an icon exists.
- For Telegram connect/disconnect UI, keep the frontend state optimistic only after the API succeeds.
- For E2E tests, mock backend API routes unless the task explicitly asks for full-stack E2E.

## Testing Expectations

Backend:

- Run `cd backend && ./mvnw test` after backend changes.
- Add or update tests near the changed behavior.
- For notification reminder/retry work, include tests for due reminders, successful delivery, failures, retry scheduling, max attempts, and duplicate prevention.

Frontend:

- Run `cd frontend && yarn lint` after frontend changes.
- Run `cd frontend && yarn test` after component/API-client logic changes.
- Run `cd frontend && yarn test:e2e` after user-flow changes such as settings, onboarding, auth, connection cards, or toast behavior.
- Keep E2E selectors user-facing where practical: roles, button names, headings, visible text.

## Coding Style

- Follow existing code style before introducing new abstractions.
- Keep changes tightly scoped to the user request.
- Do not rewrite unrelated files or reformat large areas incidentally.
- Do not revert user changes unless explicitly asked.
- Prefer explicit, readable code over clever helpers.
- Add comments only when they clarify non-obvious logic.

## Typical User-Flows to Protect

- Register/login, auth hydration, redirect after login.
- Onboarding steps: welcome, cycle history, reminders, notification channels, completion.
- Settings save and timezone update.
- Telegram connect deeplink flow:
  - User clicks connect.
  - Backend creates a pending token and returns a Telegram deep link.
  - Frontend opens Telegram.
  - User presses Start in Telegram.
  - Backend processes Telegram update and marks the channel connected.
  - Frontend polls connection status and updates UI.
- Telegram disconnect:
  - Frontend calls DELETE.
  - Backend removes/deactivates channel and returns `204`.
  - Frontend updates UI to disconnected and shows a success toast.
- Discord OAuth connect/callback/disconnect.
- Reminder cron sends due messages only once.
- Failed notification retry respects retry schedule and max attempts.

## When Unsure

- Inspect the relevant code and tests first.
- Prefer adding a small regression test before changing behavior.
- If a change touches both backend and frontend contracts, update types, API clients, translations, and tests together.
- If an external integration is disabled by env, make local behavior explicit in tests and error handling.
