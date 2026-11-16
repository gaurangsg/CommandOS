# CommandOS data model foundation

The Flyway baseline is `backend/src/main/resources/db/migration/V1__create_platform_foundation.sql`; `V2__prepare_identity_and_rbac.sql` adds refresh-token family tracking and seeds the permission catalog; `V3__add_people_and_department_management.sql` adds employee profile fields and people/department permissions; `V4__add_employee_invitations.sql` adds expiring invitations and seeds the Member role for existing workspaces. These migrations define the platform's identity, tenancy, and work-tracking foundation using MySQL 8 / InnoDB. Identifiers are UUIDs stored as `BINARY(16)`; token digests use `BINARY(32)`; timestamps use UTC `TIMESTAMP(6)`.

| Table | Responsibility | Tenant boundary / key indexes |
|---|---|---|
| `organizations` | Workspace identity and lifecycle | Unique `slug`; status/created index |
| `users` | Global login identity and profile | Unique normalized email (application must normalize); status index |
| `roles`, `permissions` | Organization roles and permission catalog | Role unique by organization/name; permission unique by key |
| `role_permissions` | Role-to-permission assignments | Composite primary key |
| `organization_memberships` | User's organization membership, role, department, manager, and employment profile | Unique organization/user; user/status and organization/department lookups |
| `refresh_tokens` | Hashed, revocable refresh-token lineage | Unique hash; user/expiry and family/revocation lookups |
| `employee_invitations` | Workspace-scoped, expiring invitations with hashed bearer tokens | Unique token hash; unique active invitee per organization; organization/status/created lookup |
| `departments` | Organization-owned hierarchy | Organization/name unique; parent lookup |
| `projects` | Organization-owned project portfolio | Organization/name unique; organization/status/date lookup |
| `tasks` | Organization-owned work items | Organization/status/due, assignee/status, and project/status indexes |
| `audit_events` | Organization-scoped immutable activity metadata | Organization/time and resource/time lookups |

Tenant-owned records carry `organization_id` and have an organization foreign key. Application queries must still include the authorized organization ID; database foreign keys alone do not provide tenant isolation. Cross-organization relationships must be validated in the service before writes. Employee department and manager assignments are organization-validated; department parent relationships are constrained to the owning organization by application services.

Migration files are immutable after deployment: add a new `Vn__description.sql` for subsequent changes. Use forward migrations and an explicit backup/restore procedure for production rollback.
