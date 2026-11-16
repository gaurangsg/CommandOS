ALTER TABLE refresh_tokens
    ADD COLUMN family_id BINARY(16) NULL AFTER user_id;

UPDATE refresh_tokens
SET family_id = id
WHERE family_id IS NULL;

ALTER TABLE refresh_tokens
    MODIFY family_id BINARY(16) NOT NULL,
    ADD KEY idx_refresh_tokens_family_revoked (family_id, revoked_at);

INSERT INTO permissions (id, permission_key, description) VALUES
    (UUID_TO_BIN(UUID()), 'organization:read', 'View organization settings'),
    (UUID_TO_BIN(UUID()), 'organization:manage', 'Manage organization settings'),
    (UUID_TO_BIN(UUID()), 'members:read', 'View organization members'),
    (UUID_TO_BIN(UUID()), 'members:manage', 'Invite and manage organization members'),
    (UUID_TO_BIN(UUID()), 'roles:manage', 'Manage organization roles and permissions'),
    (UUID_TO_BIN(UUID()), 'audit:read', 'View organization audit events');
