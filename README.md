# Snapl

Short links, built for teams that scale. Snapl is a multi-tenant URL shortener
with branded custom domains, real-time click analytics, scoped API keys, and
role-based team access — a reactive Spring Boot backend paired with a React
frontend.

## Features

- **Short links** — custom aliases, tags, folders, scheduled activation, and
  optional password protection
- **Custom domains** — brand short links with your own verified domain
- **Real-time click analytics** — Kafka-backed click event pipeline with
  daily rollups, referrer/device/browser/OS breakdowns
- **Multi-tenant organizations** — Owner/Admin/Member roles, JWT access +
  refresh token auth
- **API access** — scoped (read/write) API keys for programmatic link
  creation
- **Billing** — Stripe-backed plans (Free / Pro / Business) with usage limits
  enforced per organization
- **Hardened for production** — Redis-backed rate limiting and long-URL
  deduplication

## Tech stack

**Backend** — Java 21, Spring Boot 4 (WebFlux, fully reactive/non-blocking),
Spring Data R2DBC + Flyway migrations, PostgreSQL, Redis, Apache Kafka,
Stripe API.

**Frontend** — React 19, TypeScript, Vite, Tailwind CSS v4, TanStack Query,
React Router, Recharts, Radix UI primitives.

## Project structure

```
src/main/java/com/satyam/urlshortner/   Backend source (auth, url, domain,
                                         analytics, billing, apikey packages)
src/main/resources/db/migration/        Flyway SQL migrations
frontend/                               React frontend (Vite)
docker-compose.yml                      Postgres, Redis, Kafka, and the app
docs/superpowers/                       Design specs and implementation plans
```

## Getting started

### Prerequisites

- Java 21+
- Node.js 20+
- Docker (for Postgres, Redis, and Kafka)

### 1. Start infrastructure

```bash
docker compose up -d postgres redis kafka
```

This starts Postgres on `localhost:15432`, Redis on `localhost:6379`, and
Kafka on `localhost:9092`.

### 2. Run the backend

```bash
./mvnw spring-boot:run
```

Flyway runs migrations automatically on startup. The API listens on
`http://localhost:8080`; check `http://localhost:8080/actuator/health`.

### 3. Run the frontend

```bash
cd frontend
npm install
npm run dev
```

The app runs on `http://localhost:5173` and talks to the backend at
`http://localhost:8080` (configurable via `VITE_API_BASE_URL`).

### Running everything in Docker

```bash
docker compose up --build
```

This also builds and runs the backend container alongside its dependencies.

## Configuration

Key environment variables (see `src/main/resources/application.yaml` for the
full list and defaults):

| Variable | Purpose |
|---|---|
| `APP_BASE_URL` | Public base URL used when generating short links |
| `FRONTEND_ORIGIN` | Allowed CORS origin for the frontend |
| `JWT_SECRET` | Signing secret for access/refresh tokens (32+ bytes) |
| `RATE_LIMIT_CAPACITY` / `RATE_LIMIT_REFILL_PER_SECOND` | Redis token-bucket rate limiting |
| `STRIPE_SECRET_KEY` / `STRIPE_WEBHOOK_SECRET` | Stripe billing integration |
| `WORKER_ID` | Snowflake ID worker id — must be unique per running instance |
