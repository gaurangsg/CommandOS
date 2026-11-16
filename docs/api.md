# API conventions

Base path: `/api/v1`. JSON uses camelCase, UUID identifiers, ISO-8601 UTC timestamps, and request DTO validation. Hibernate entities are never serialized.

## Response envelope

```json
{
  "success": true,
  "data": { "status": "UP", "service": "CommandOS API" },
  "timestamp": "2026-01-01T00:00:00Z"
}
```

Errors use a stable envelope with a machine-readable code and safe message. List endpoints will use explicit `page`, `size`, `sort`, and module-specific filters with bounded page sizes.

## Implemented endpoints

| Method | Path | Purpose | Access |
|---|---|---|---|
| `GET` | `/api/v1/health` | API liveness check | Public |
| `POST` | `/api/v1/auth/register` | Create an organization, owner account, and session | Public |
| `POST` | `/api/v1/auth/login` | Start a session for an email/password/workspace slug | Public |
| `POST` | `/api/v1/auth/refresh` | Rotate the HttpOnly refresh cookie and issue a new access token; request body contains `organizationSlug` | Refresh cookie |
| `POST` | `/api/v1/auth/logout` | Revoke the current refresh-token family and clear the cookie | Public; cookie is revoked when present |
| `GET` | `/api/v1/auth/invitations/preview?token=...` | Validate a pending invitation and return workspace, invitee, department, and expiry details | Public; response is not cacheable |
| `POST` | `/api/v1/auth/invitations/accept` | Accept an invitation, create the user/member account, and return a session | Public; single-use invitation token |
| `GET` | `/api/v1/auth/me` | Return the authenticated identity and organization membership | Bearer JWT |
| `GET` | `/api/v1/organizations/current` | Return the authenticated organization | `organization:read` permission |
| `GET` | `/api/v1/people` | Search and page through active workspace employees; filters: `search`, `departmentId` | `people:read` permission |
| `PUT` | `/api/v1/people/{userId}` | Update an employee's job title, department, manager, employment type, and start date | `people:manage` permission |
| `GET` | `/api/v1/departments` | List workspace departments and active-member counts | `departments:read` permission |
| `POST` | `/api/v1/departments` | Create a department, optionally under a parent | `departments:manage` permission |
| `PUT` | `/api/v1/departments/{departmentId}` | Rename or move a department in the workspace hierarchy | `departments:manage` permission |
| `DELETE` | `/api/v1/departments/{departmentId}` | Delete an empty department with no child departments | `departments:manage` permission |
| `GET` | `/api/v1/people/invitations` | List invitations for the authenticated workspace | `people:manage` permission |
| `POST` | `/api/v1/people/invitations` | Email an invitation for the default Member role; optionally assign a department | `people:manage` permission |
| `DELETE` | `/api/v1/people/invitations/{invitationId}` | Revoke a pending invitation | `people:manage` permission |

Registration JSON fields: `displayName`, `email`, `password` (12–72 characters and at most 72 UTF-8 bytes), `organizationName`, and lowercase `organizationSlug`. Login requires `email`, `password`, and `organizationSlug`. Access tokens are returned in JSON; refresh tokens are never returned in JSON.

People and department endpoints derive the organization scope from the verified access token. Employee profile writes reject departments and managers outside that organization; department parent references are likewise tenant-validated. Employee listing accepts zero-based `page` (default `0`), `size` (default `20`, maximum `100`), `search`, and `departmentId`.

The employee profile `PUT` body contains `jobTitle`, `departmentId`, `managerUserId`, `employmentType` (`FULL_TIME`, `PART_TIME`, `CONTRACTOR`, or `INTERN`), and `startDate` (ISO date); nullable fields may be sent as `null`. Department create/update bodies contain `name` and nullable `parentDepartmentId`. A department can be deleted only when it has no active employee assignments or child departments.

Invitation create JSON fields are `email`, `displayName`, and nullable `departmentId`. Acceptance JSON fields are `token`, `displayName`, and `password` (at least 12 characters and no more than 72 UTF-8 bytes). The raw random invitation token is only emailed; the database stores its SHA-256 hash. Invitations expire after seven days, can be accepted once, and can be revoked while pending. New accounts are assigned the workspace's standard `Member` role. This flow currently rejects email addresses that already have a CommandOS account.

Invitation delivery requires SMTP configuration via `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`, `MAIL_SMTP_AUTH`, and `MAIL_SMTP_STARTTLS`; `FRONTEND_BASE_URL` controls the acceptance link. Missing SMTP settings and send failures return `503 Service Unavailable` and leave no pending invitation.

OpenAPI UI: `/swagger-ui.html`; specification: `/v3/api-docs`. Remaining business-module endpoints will be documented as their tenant-scoped implementations are added.
