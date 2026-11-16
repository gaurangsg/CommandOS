# CommandOS architecture

## System shape

CommandOS is a modular monolith: one deployable Spring Boot application, one relational database, and explicit package boundaries. Modules can be split into services later only if product scale and team ownership justify it.

```text
React SPA ── HTTPS/JSON ── Spring Boot API
                               ├── platform (configuration, health, shared API)
                               ├── organization (tenant and membership boundary)
                               ├── identity (authentication and authorization)
                               ├── workforce (people and departments)
                               ├── projects (projects and tasks)
                               ├── workflows (automation and approvals)
                               ├── finance (expenses and budgets)
                               ├── resources (procurement, vendors, assets)
                               ├── notifications (in-app and WebSocket delivery)
                               └── insights (analytics and audit views)
                                     │
                              MySQL 8 + Flyway
                              Redis (later, as justified)
```

## Backend conventions

Each business module owns its controllers, DTOs, services, repositories, and persistence model. The intended request flow is Controller → Service → Repository → Entity. Controllers accept and return DTOs only. Shared code belongs in `com.commandos.shared`; operational and cross-cutting configuration belongs in `com.commandos.platform`.

Use organization-scoped service/repository methods for every tenant-owned record. Resolve the organization from the authenticated principal, not a caller-controlled request parameter. Apply authorization at the service boundary as well as the HTTP route. The initial migration places `organization_id` on tenant-owned tables and indexes common organization-scoped access paths; this is a foundation, not a substitute for authorization checks.

Schema evolution is Flyway-owned. Hibernate runs with `ddl-auto: validate`; schema auto-creation is not enabled. API payloads use a consistent envelope and UTC timestamps. OpenAPI is available at `/v3/api-docs` and `/swagger-ui.html`.

## Frontend conventions

`src/pages` owns route-level screens, `src/components` shared UI, `src/api` typed HTTP access and response validation, and `src/stores` cross-screen client preferences. TanStack Query owns server state; Zustand is reserved for local UI preferences. MUI theme tokens define light/dark visual modes.

## Security and deployment decisions

- Tenant isolation is a backend invariant; never trust a tenant identifier supplied by a browser.
- Registration creates a workspace, an active owner membership, and a role with the seeded owner permissions in one transaction. Login requires the workspace slug and always returns a generic invalid-credentials message.
- Access JWTs use HS256 with a required environment-supplied key (minimum 32 bytes), a 15-minute default lifetime, issuer validation, and organization/permission claims. The browser keeps access tokens in memory; refresh tokens are high-entropy opaque values stored only as SHA-256 hashes and delivered in HttpOnly, SameSite=Strict cookies.
- Refresh rotates tokens under a row lock. Reuse of a rotated token revokes its active token family. Refresh tokens never appear in JSON responses. Enable secure cookies in HTTPS deployments.
- Permission claims are mapped to Spring Security authorities; organization routes derive their tenant from signed token claims and enforce permission checks. Tenant-owned services must continue to scope repository queries by that organization ID.
- Keep secrets in deployment environment configuration or a managed secret store, not source control.
- CORS is restricted to the configured frontend origin. Health, API documentation, and auth session establishment/rotation routes are public; workspace APIs require a valid access token.
- MySQL and Redis are optional, portable Compose services. Redis is included for later use but is not yet a runtime dependency.
- Use S3-compatible object storage behind an application-owned storage abstraction if/when uploads are implemented.
