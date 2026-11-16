ALTER TABLE organization_memberships
    ADD COLUMN department_id BINARY(16) NULL AFTER role_id,
    ADD COLUMN job_title VARCHAR(120) NULL,
    ADD COLUMN manager_user_id BINARY(16) NULL,
    ADD COLUMN employment_type VARCHAR(24) NOT NULL DEFAULT 'FULL_TIME',
    ADD COLUMN start_date DATE NULL,
    ADD KEY idx_memberships_organization_department (organization_id, department_id),
    ADD CONSTRAINT fk_memberships_department
        FOREIGN KEY (department_id) REFERENCES departments (id),
    ADD CONSTRAINT fk_memberships_manager
        FOREIGN KEY (manager_user_id) REFERENCES users (id);

INSERT INTO permissions (id, permission_key, description) VALUES
    (UUID_TO_BIN(UUID()), 'people:read', 'View organization people'),
    (UUID_TO_BIN(UUID()), 'people:manage', 'Manage employee profiles'),
    (UUID_TO_BIN(UUID()), 'departments:read', 'View organization departments'),
    (UUID_TO_BIN(UUID()), 'departments:manage', 'Manage organization departments');

INSERT INTO role_permissions (role_id, permission_id)
SELECT role.id, permission.id
FROM roles role
JOIN permissions permission
    ON permission.permission_key IN (
        'people:read',
        'people:manage',
        'departments:read',
        'departments:manage'
    )
WHERE role.name = 'Owner';
