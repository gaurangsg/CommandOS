package com.commandos.organization.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "permissions")
public class PermissionEntity {

    @Id
    private UUID id;

    @Column(name = "permission_key", nullable = false, unique = true, length = 120)
    private String permissionKey;

    protected PermissionEntity() {
    }

    public String getPermissionKey() {
        return permissionKey;
    }
}
