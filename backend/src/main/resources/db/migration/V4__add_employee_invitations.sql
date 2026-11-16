CREATE TABLE employee_invitations (
    id BINARY(16) NOT NULL,
    organization_id BINARY(16) NOT NULL,
    invited_email VARCHAR(254) NOT NULL,
    display_name VARCHAR(160) NOT NULL,
    role_id BINARY(16) NOT NULL,
    department_id BINARY(16) NULL,
    invited_by_user_id BINARY(16) NOT NULL,
    token_hash BINARY(32) NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'PENDING',
    expires_at TIMESTAMP(6) NOT NULL,
    accepted_at TIMESTAMP(6) NULL,
    revoked_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    active_email VARCHAR(254)
        GENERATED ALWAYS AS (CASE WHEN status = 'PENDING' THEN invited_email ELSE NULL END) STORED,
    PRIMARY KEY (id),
    UNIQUE KEY uk_employee_invitation_token_hash (token_hash),
    UNIQUE KEY uk_employee_invitation_active_email (organization_id, active_email),
    KEY idx_employee_invitations_org_status (organization_id, status, created_at),
    CONSTRAINT fk_employee_invitations_organization
        FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_employee_invitations_role
        FOREIGN KEY (role_id) REFERENCES roles (id),
    CONSTRAINT fk_employee_invitations_department
        FOREIGN KEY (department_id) REFERENCES departments (id),
    CONSTRAINT fk_employee_invitations_inviter
        FOREIGN KEY (invited_by_user_id) REFERENCES users (id)
) ENGINE=InnoDB;

INSERT INTO roles (id, organization_id, name, description, is_system)
SELECT UUID_TO_BIN(UUID()), organization.id, 'Member', 'Standard workspace member', TRUE
FROM organizations organization
WHERE NOT EXISTS (
    SELECT 1 FROM roles role
    WHERE role.organization_id = organization.id AND role.name = 'Member'
);

INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT role.id, permission.id
FROM roles role
JOIN permissions permission
    ON permission.permission_key IN ('organization:read', 'people:read', 'departments:read')
WHERE role.name = 'Member';
