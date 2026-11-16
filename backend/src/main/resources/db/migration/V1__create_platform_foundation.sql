CREATE TABLE organizations (
    id BINARY(16) NOT NULL,
    name VARCHAR(160) NOT NULL,
    slug VARCHAR(80) NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_organizations_slug (slug),
    KEY idx_organizations_status_created (status, created_at)
) ENGINE=InnoDB;

CREATE TABLE users (
    id BINARY(16) NOT NULL,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(160) NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'INVITED',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email),
    KEY idx_users_status_created (status, created_at)
) ENGINE=InnoDB;

CREATE TABLE roles (
    id BINARY(16) NOT NULL,
    organization_id BINARY(16) NOT NULL,
    name VARCHAR(80) NOT NULL,
    description VARCHAR(255) NULL,
    is_system BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    UNIQUE KEY uk_roles_organization_name (organization_id, name),
    CONSTRAINT fk_roles_organization FOREIGN KEY (organization_id) REFERENCES organizations (id)
) ENGINE=InnoDB;

CREATE TABLE permissions (
    id BINARY(16) NOT NULL,
    permission_key VARCHAR(120) NOT NULL,
    description VARCHAR(255) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_permissions_key (permission_key)
) ENGINE=InnoDB;

CREATE TABLE role_permissions (
    role_id BINARY(16) NOT NULL,
    permission_id BINARY(16) NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES roles (id),
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions (id)
) ENGINE=InnoDB;

CREATE TABLE organization_memberships (
    id BINARY(16) NOT NULL,
    organization_id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    role_id BINARY(16) NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'INVITED',
    joined_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_membership_organization_user (organization_id, user_id),
    KEY idx_memberships_user_status (user_id, status),
    CONSTRAINT fk_memberships_organization FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_memberships_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_memberships_role FOREIGN KEY (role_id) REFERENCES roles (id)
) ENGINE=InnoDB;

CREATE TABLE refresh_tokens (
    id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    token_hash BINARY(32) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    revoked_at TIMESTAMP(6) NULL,
    replaced_by_token_id BINARY(16) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_refresh_tokens_hash (token_hash),
    KEY idx_refresh_tokens_user_expiry (user_id, expires_at),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_refresh_tokens_replacement FOREIGN KEY (replaced_by_token_id) REFERENCES refresh_tokens (id)
) ENGINE=InnoDB;

CREATE TABLE departments (
    id BINARY(16) NOT NULL,
    organization_id BINARY(16) NOT NULL,
    parent_department_id BINARY(16) NULL,
    name VARCHAR(120) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_departments_organization_name (organization_id, name),
    KEY idx_departments_parent (organization_id, parent_department_id),
    CONSTRAINT fk_departments_organization FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_departments_parent FOREIGN KEY (parent_department_id) REFERENCES departments (id)
) ENGINE=InnoDB;

CREATE TABLE projects (
    id BINARY(16) NOT NULL,
    organization_id BINARY(16) NOT NULL,
    name VARCHAR(180) NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'PLANNED',
    start_date DATE NULL,
    target_date DATE NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_projects_organization_name (organization_id, name),
    KEY idx_projects_organization_status_date (organization_id, status, target_date),
    CONSTRAINT fk_projects_organization FOREIGN KEY (organization_id) REFERENCES organizations (id)
) ENGINE=InnoDB;

CREATE TABLE tasks (
    id BINARY(16) NOT NULL,
    organization_id BINARY(16) NOT NULL,
    project_id BINARY(16) NULL,
    title VARCHAR(240) NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'BACKLOG',
    priority VARCHAR(16) NOT NULL DEFAULT 'NORMAL',
    assignee_user_id BINARY(16) NULL,
    due_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_tasks_organization_status_due (organization_id, status, due_at),
    KEY idx_tasks_organization_assignee_status (organization_id, assignee_user_id, status),
    KEY idx_tasks_project_status (organization_id, project_id, status),
    CONSTRAINT fk_tasks_organization FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_tasks_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_tasks_assignee FOREIGN KEY (assignee_user_id) REFERENCES users (id)
) ENGINE=InnoDB;

CREATE TABLE audit_events (
    id BINARY(16) NOT NULL,
    organization_id BINARY(16) NOT NULL,
    actor_user_id BINARY(16) NULL,
    action VARCHAR(120) NOT NULL,
    resource_type VARCHAR(80) NOT NULL,
    resource_id BINARY(16) NULL,
    occurred_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_audit_organization_time (organization_id, occurred_at),
    KEY idx_audit_resource (organization_id, resource_type, resource_id, occurred_at),
    CONSTRAINT fk_audit_organization FOREIGN KEY (organization_id) REFERENCES organizations (id),
    CONSTRAINT fk_audit_actor FOREIGN KEY (actor_user_id) REFERENCES users (id)
) ENGINE=InnoDB;
