# Delivery progress

Updated: 2026-11-15

## Phase 1 — Platform foundation

- [x] Establish frontend/backend project boundaries and local configuration.
- [x] Define modular-monolith boundaries and backend API conventions.
- [x] Add MySQL 8 Flyway baseline schema and Hibernate schema validation.
- [x] Create responsive CommandOS dashboard shell with light/dark preference.
- [x] Connect the UI to the real API health endpoint; show empty states rather than fabricated business metrics.
- [x] Add a focused API health unit test.
- [x] Run backend build and test on Java 21.
- [x] Complete frontend production-build and HTTP preview smoke tests.
- [x] Apply the Flyway migration against the configured local MySQL 8 database and verify the API health endpoint.

## Phase 2 — Authentication and workspace isolation

- [x] Add Flyway identity/RBAC seed migration and validate it against the configured MySQL database.
- [x] Implement organization registration, owner membership, login, logout, and authenticated current-user/workspace APIs.
- [x] Add signed access tokens, rotating HttpOnly refresh cookies, refresh-token replay family revocation, and permission-based endpoint authorization.
- [x] Build validated login and workspace-registration forms with protected routing and authenticated workspace/profile UI.
- [x] Verify registration, login, logout, tenant endpoint access, cookie behavior, invalid credentials, token rotation/reuse, and anonymous denial through API smoke tests.
- [x] Verify browser registration → dashboard → logout → login against the live API; remove the disposable test workspace and user.
- [x] Run `mvn verify` (5 tests) and `npm run build`.

## Phase 3 — People and departments

- [x] Add employee profile fields and people/department permission seeds with Flyway V3; apply the migration to local MySQL 8.
- [x] Implement tenant-scoped employee listing, pagination/search/filtering, profile updates, and manager/department validation.
- [x] Implement department hierarchy listing, creation, updates, and safe deletion; prevent cross-tenant parents and hierarchy cycles.
- [x] Connect the responsive People page to live APIs, including employee profile and department forms; show the live member count on the dashboard.
- [x] Verify two-organization isolation, authorization, profile and department operations, invalid managers/parents, hierarchy cycles, deletion constraints, and request validation.
- [x] Run backend verification (12 tests), frontend production build, and browser registration/login/department/profile checks; remove disposable test data.
- [x] Add tenant-scoped employee invitations with SMTP delivery, hashed single-use tokens, seven-day expiry, revocation, and default Member role assignment.
- [x] Add public invitation preview/acceptance APIs and the responsive frontend invitation/activation flow; reject account conflicts and report SMTP misconfiguration/delivery failures explicitly.
- [x] Add SMTP configuration guidance, invitation API/database documentation, and focused invitation security/expiry tests.
- [x] Apply Flyway V4 to local MySQL and verify startup/schema validation, API health, public invalid-token handling, and anonymous denial of invitation management.

## Planned phases

1. Foundation — implementation, build, and local MySQL migration verification complete.
2. Authentication, organization onboarding, tenant isolation, and RBAC — implementation and targeted verification complete.
3. Employees, departments, and organizational management — implementation, 12 backend tests, frontend build, Flyway V4, and API smoke checks complete. SMTP provider delivery requires the deployment's configured mail settings and was not exercised against an external provider.
4. Projects and tasks, including Kanban.
5. Workflows and approvals.
6. Expenses, budgets, and notifications.
7. Analytics, audit interfaces, and justified Redis caching.
8. Integration testing, containers, CI/CD, and deployment.

No phase is considered complete until its implemented behavior has passed targeted tests.
