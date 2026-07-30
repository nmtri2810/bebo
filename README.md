# Bebo

Bebo is a cycle tracking and reminder app. It lets users record cycle start dates, predict the next expected cycle, and receive reminders through Telegram or Discord.

This repo contains:

- `backend/`: Spring Boot backend with PostgreSQL, Flyway, JWT auth, notification channels, scheduled reminders, and retry handling.
- `frontend/`: Next.js frontend with React, TypeScript, Tailwind CSS, next-intl, Zustand, Sonner, Vitest, and Playwright.
- `compose.yaml`: local PostgreSQL service.
- `docs/`: project knowledge for humans and Codex.
- `AGENTS.md`: root instructions for Codex.

## Local Setup

Start PostgreSQL:

```bash
docker compose up -d postgres
```

Run backend:

```bash
cd backend
./mvnw spring-boot:run
```

Run frontend:

```bash
cd frontend
yarn dev
```

Default local URLs:

- Frontend: `http://localhost:3000`
- Backend: `http://localhost:8080`
- PostgreSQL host port: `5433`

## Environment

Backend reads `../.env` optionally. Useful local defaults are already present in `backend/src/main/resources/application.yml`.

Frontend uses:

```bash
NEXT_PUBLIC_API_URL=http://localhost:8080
```

Telegram and Discord are disabled by default. Enable only with local secrets:

- `TELEGRAM_BOT_ENABLED=true`
- `TELEGRAM_BOT_TOKEN`
- `TELEGRAM_BOT_USERNAME`
- `DISCORD_BOT_ENABLED=true`
- `DISCORD_CLIENT_ID`
- `DISCORD_CLIENT_SECRET`
- `DISCORD_BOT_TOKEN`

Never commit real secrets.

## Test Commands

Backend:

```bash
cd backend
./mvnw test
```

Frontend:

```bash
cd frontend
yarn lint
yarn test
yarn test:e2e
```

If Playwright browsers are missing:

```bash
cd frontend
yarn playwright install chromium
```

## Docs

- `docs/architecture.md`: system shape and request flows.
- `docs/domain-rules.md`: product and domain rules.
- `docs/database.md`: schema and migration guidance.
- `docs/testing.md`: test strategy.
- `.agent/PLANS.md`: long-running implementation plan log.

Codex should read `AGENTS.md` first, then the relevant docs and nested `AGENTS.md` files for the area being changed.
