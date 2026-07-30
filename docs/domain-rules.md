# Domain Rules

## Product Scope

Bebo helps users track cycle dates and receive reminders. It is not a medical, diagnostic, contraception, fertility, or mental health product.

User-facing language should stay careful:

- Prefer "estimated" or "expected" dates.
- Avoid medical certainty.
- Avoid judging mood, behavior, or emotions.
- Do not imply the app can diagnose health conditions.

## Cycle Prediction

- A cycle record represents a user-entered cycle start date.
- The latest cycle start date is the anchor for the next prediction.
- If there is not enough history, use the user's default cycle length.
- When enough history exists, prediction can use observed cycle lengths.
- Prediction output is an estimate and should be treated as advisory.

## Settings

Each user owns settings for:

- Default cycle length.
- Reminder days before expected cycle start.
- Notification time.
- Timezone.

Timezones should be IANA timezone strings such as `Asia/Ho_Chi_Minh`.

## Reminder Planning

A reminder plan is based on:

- Latest known cycle start date.
- Predicted next cycle start date.
- User reminder days before.
- User notification time.
- User timezone.

Reminder jobs should be idempotent. A reminder already sent for the same intended cycle occurrence must not be sent again.

## Notification Channels

Supported channel types:

- `TELEGRAM`
- `DISCORD`

Supported channel statuses:

- `DISCONNECTED`
- `PENDING`
- `CONNECTED`
- `ALREADY_LINKED`

A connected external account should belong to only one Bebo user at a time. If an external account is already linked to another user, surface the `ALREADY_LINKED` state instead of silently relinking it.

## Telegram Rules

- Telegram is disabled unless `TELEGRAM_BOT_ENABLED=true`.
- Connect starts from the frontend and returns a deep link.
- The deep link is completed when the user presses Start in Telegram.
- Frontend polling is expected during `PENDING`.
- Disconnect should return `204 No Content`.
- Sending a test notification requires a connected Telegram channel.

## Discord Rules

- Discord is disabled unless `DISCORD_BOT_ENABLED=true`.
- Connect uses OAuth state.
- Callback results should be mapped to clear frontend states/messages.
- Disconnect should return `204 No Content`.
- Sending a test notification requires a connected Discord channel.

## Retry Rules

Notification failures should capture retry metadata:

- Attempt count.
- Next retry time.
- Failure reason.
- Final failure when max attempts is reached.

Retry processors should only retry eligible failed logs and should not retry already sent notifications.

## UX Rules

- Use Sonner toasts for action success/failure feedback.
- Avoid duplicate inline success messages when a toast already confirms the action.
- Keep inline errors when they help the user understand or recover.
- Telegram/Discord connection cards should update only after successful API responses.
