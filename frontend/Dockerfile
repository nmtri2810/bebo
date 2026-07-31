# syntax=docker/dockerfile:1

# =========================================================
# Stage 1: Install dependencies
# =========================================================
FROM node:22-bookworm-slim AS dependencies

WORKDIR /app

RUN corepack enable

COPY package.json yarn.lock .yarnrc.yml ./

RUN yarn install --immutable


# =========================================================
# Stage 2: Build Next.js application
# =========================================================
FROM node:22-bookworm-slim AS builder

WORKDIR /app

ENV NEXT_TELEMETRY_DISABLED=1

RUN corepack enable

COPY --from=dependencies /app/node_modules ./node_modules

COPY . .

# NEXT_PUBLIC variables được đóng vào frontend bundle
# tại thời điểm build.
ARG NEXT_PUBLIC_API_URL=""

ENV NEXT_PUBLIC_API_URL=${NEXT_PUBLIC_API_URL}

# Project hiện chưa có thư mục public.
# Tạo sẵn để bước copy ở runtime luôn hoạt động.
RUN mkdir -p public \
    && yarn build


# =========================================================
# Stage 3: Production runtime
# =========================================================
FROM node:22-bookworm-slim AS runner

WORKDIR /app

ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1
ENV HOSTNAME=0.0.0.0
ENV PORT=3000

RUN groupadd --system --gid 1001 nodejs \
    && useradd --system \
        --uid 1001 \
        --gid nodejs \
        nextjs

COPY --from=builder \
    --chown=nextjs:nodejs \
    /app/.next/standalone ./

COPY --from=builder \
    --chown=nextjs:nodejs \
    /app/.next/static ./.next/static

COPY --from=builder \
    --chown=nextjs:nodejs \
    /app/public ./public

USER nextjs

EXPOSE 3000

CMD ["node", "server.js"]
