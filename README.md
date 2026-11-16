# CommandOS

**One Workspace. Every Operation. Complete Control.**

CommandOS is an enterprise operations platform built as a modular monolith. This repository starts with the Phase 1 platform foundation: a React/TypeScript workspace shell, a Java 21/Spring Boot API, a versioned MySQL schema, and portable local service configuration.

> **Current delivery:** Phases 1–3 are implemented, including employee/department management and email invitations. Project and task management is next. SMTP delivery requires the provider settings documented below; dashboard figures are sourced from the API rather than fabricated.

## Architecture

- `frontend/` — React, TypeScript, MUI, React Router, TanStack Query, Zustand, Recharts, React Flow, Framer Motion, and Zod.
- `backend/` — Java 21, Spring Boot 3, Spring Security, Spring Data JPA, Bean Validation, Flyway, and OpenAPI.
- `backend/src/main/resources/db/migration/` — MySQL 8-compatible, forward-only Flyway migrations.
- `compose.yaml` — optional local MySQL and Redis services; no cloud vendor is required.
- `docs/` — architecture, data model, API conventions, and delivery status.

## Run locally

Prerequisites: Node.js 20+, Java 21, Maven 3.9+, and Docker with the Compose plugin for local MySQL.

1. Copy `.env.example` to `.env`; set local-only database passwords. Docker Compose reads this file automatically.
2. Start the database: `docker compose up -d mysql`.
3. Configure `DATABASE_URL`, `DATABASE_USERNAME`, and `DATABASE_PASSWORD` in the API process environment (or IDE run configuration) to match the MySQL service. The PowerShell example below loads the root `.env` into the current terminal session.
4. Start the API: from `backend/`, run `mvn spring-boot:run`.
5. Start the web app: from `frontend/`, run `npm install` once and `npm run dev`.
6. Open `http://localhost:5173`. The API health check is at `http://localhost:8080/api/v1/health`; Swagger UI is at `http://localhost:8080/swagger-ui.html`.

PowerShell example for steps 3–4:

```powershell
$settings = @{}
Get-Content ..\.env | ForEach-Object {
    if ($_ -match '^\s*([^#=\s]+)\s*=\s*(.*?)\s*$') {
        $settings[$matches[1]] = $matches[2].Trim('"').Trim("'")
    }
}
$env:DATABASE_URL = $settings['DATABASE_URL']
$env:DATABASE_USERNAME = $settings['DATABASE_USERNAME']
if (-not $env:DATABASE_USERNAME) { $env:DATABASE_USERNAME = $settings['MYSQL_USER'] }
$env:DATABASE_PASSWORD = $settings['DATABASE_PASSWORD']
if (-not $env:DATABASE_PASSWORD) { $env:DATABASE_PASSWORD = $settings['MYSQL_PASSWORD'] }
$env:JWT_SECRET = $settings['JWT_SECRET']
$env:JWT_ACCESS_TOKEN_SECONDS = $settings['JWT_ACCESS_TOKEN_SECONDS']
$env:JWT_REFRESH_TOKEN_DAYS = $settings['JWT_REFRESH_TOKEN_DAYS']
$env:AUTH_COOKIE_SECURE = $settings['AUTH_COOKIE_SECURE']
Set-Location backend
mvn spring-boot:run
```

The API requires `JWT_SECRET` (at least 32 bytes). Generate one locally with `node -e "console.log(require('crypto').randomBytes(32).toString('base64url'))"` and keep it only in `.env` or your deployment secret store. Set `AUTH_COOKIE_SECURE=true` behind HTTPS. The web app accepts `VITE_API_BASE_URL`; the API accepts the database, JWT, `SERVER_PORT`, `FRONTEND_ORIGIN`, and cookie settings above. Never use example credentials or keys outside local development.

Employee invitations use the configured SMTP provider. Set `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`, `MAIL_SMTP_AUTH`, and `MAIL_SMTP_STARTTLS`, and set `FRONTEND_BASE_URL` to the public frontend origin. Invitation creation returns a clear service-unavailable error when delivery is not configured or fails; it does not report an unsent invitation as successful.

The database applies Flyway migrations at startup. V4 adds single-use employee invitations and seeds the standard Member role for existing workspaces. Newly registered workspaces receive the same role and read-only people/department permissions.

## Verification

- Frontend production build: `cd frontend && npm run build`
- Backend tests and packaging: `cd backend && mvn verify`

See [docs/progress.md](docs/progress.md) for phase status and [docs/architecture.md](docs/architecture.md) for boundaries and security decisions.
"# CommandOS" 
