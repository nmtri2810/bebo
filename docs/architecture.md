# Architecture

## System Shape

Bebo is split into two deployable applications:

- Backend: Spring Boot REST API.
- Frontend: Next.js app that calls the backend over HTTP.

PostgreSQL is the source of truth. Flyway owns schema changes. Telegram and Discord are external delivery providers.

```text
Browser
  |
  v
Next.js frontend
  |
  v
Spring Boot backend
  |
  v
PostgreSQL

Spring Boot backend
  |-- Telegram Bot API
  |-- Discord OAuth/API
```

## Backend Responsibilities

- Authentication and JWT issuing.
- User profile and onboarding state.
- Cycle records and prediction.
- Settings for cycle length, reminder days, notification time, and timezone.
- Notification channel connection state.
- Telegram deep link connect flow and update polling.
- Discord OAuth connect/callback flow.
- Scheduled reminder processing.
- Failed notification retry.
- Notification history.

## Frontend Responsibilities

- Login/register and auth hydration.
- Onboarding flow.
- Dashboard and settings screens.
- Telegram and Discord connect/disconnect UI.
- Notification history UI.
- User-facing success/error feedback with Sonner toasts.
- Localized copy in English and Vietnamese.

## Auth Flow

1. User logs in or registers from the frontend.
2. Backend returns an access token and user summary.
3. Frontend stores the session in Zustand persisted storage.
4. `AuthHydrator` rehydrates the session and refreshes current user data.
5. Authenticated API calls send `Authorization: Bearer <token>`.
6. On `401`, frontend clears the session and redirects to `/`.

## Telegram Connect Flow

1. Frontend calls `POST /api/notification-channels/telegram/connect`.
2. Backend creates a pending connection token and returns a Telegram deep link.
3. Frontend opens Telegram in a new tab/app.
4. User presses Start in Telegram.
5. Backend processes Telegram updates and completes the pending channel.
6. Frontend polls `GET /api/notification-channels/telegram`.
7. When status becomes `CONNECTED`, frontend clears the deep link and shows a success toast.

## Telegram Disconnect Flow

1. Frontend calls `DELETE /api/notification-channels/telegram`.
2. Backend disconnects the channel and returns `204 No Content`.
3. Frontend treats empty `204` as success.
4. Frontend updates the card to disconnected and shows a Sonner toast.

## Discord Connect Flow

1. Frontend calls `POST /api/notification-channels/discord/connect`.
2. Backend creates an OAuth state and returns an authorization URL.
3. Frontend redirects the user to Discord.
4. Discord redirects to the backend callback.
5. Backend exchanges the code, stores connection state, and redirects to the frontend.
6. Frontend reads callback result from query params and shows a toast/notice.

## Reminder Flow

1. Scheduler triggers `CycleReminderProcessor`.
2. Processor finds users with due reminder plans.
3. For each connected channel, backend dispatches a delivery request.
4. Success creates or updates a notification log as sent.
5. Failure creates or updates a notification log with retry metadata.
6. Retry scheduler later runs `FailedNotificationRetryProcessor`.

## Cross-Cutting Rules

- Keep business rules in backend services.
- Keep frontend API access under `frontend/src/lib/api`.
- Keep DTO/type contracts aligned across backend responses and frontend types.
- Do not log secrets, bot tokens, OAuth codes, JWTs, or Telegram start tokens.
- Keep external integration behavior explicit when env flags disable providers.
