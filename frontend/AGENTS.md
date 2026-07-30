# Frontend Guide for Codex

This directory contains the Next.js frontend for Bebo.

## Stack

- Next.js 16.
- React 19.
- TypeScript.
- Tailwind CSS.
- next-intl.
- Zustand persisted auth store.
- Sonner toasts.
- Vitest and Testing Library.
- Playwright.

## Commands

Run from `frontend/`:

- Dev server: `yarn dev`
- Lint: `yarn lint`
- Unit tests: `yarn test`
- E2E tests: `yarn test:e2e`
- Build: `yarn build`

Playwright starts its own dev server on port `3100`.

## Code Organization

- `src/app`: route-level pages and layouts.
- `src/components`: shared app components.
- `src/components/ui`: low-level UI primitives.
- `src/features`: feature-specific components.
- `src/lib/api`: typed backend API clients.
- `src/stores`: Zustand stores.
- `src/types`: frontend TypeScript API/domain types.
- `messages`: localized copy.
- `e2e`: Playwright specs.

## API Rules

- Keep backend calls in `src/lib/api`.
- Use `apiRequest` so auth headers, empty `204` responses, and errors behave consistently.
- Keep frontend types aligned with backend DTOs.
- On `401`, clear auth state and return the user to the auth screen.

## UX Rules

- Use Sonner for action feedback.
- Keep the global toaster bottom-right.
- Do not duplicate success feedback inline when a toast already confirms the action.
- Inline errors are acceptable when the user needs context or recovery.
- Buttons and clickable controls should have pointer cursor and clear disabled states.
- Keep copy localized in both `messages/en.json` and `messages/vi.json`.

## Testing Expectations

- Run `yarn lint` after frontend changes.
- Run `yarn test` after API-client, component, store, or utility changes.
- Run `yarn test:e2e` after user-flow changes.
- Use role/text selectors in Playwright when possible.
- Mock backend API routes in E2E unless explicitly asked for full-stack E2E.

## Next.js Caution

Before changing app routing, server/client boundaries, metadata, layouts, cookies, or config, inspect the local Next.js docs under `node_modules/next/dist/docs/` if available.
