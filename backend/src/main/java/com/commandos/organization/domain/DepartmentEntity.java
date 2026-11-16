package com.commandos.organization.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "departments")
public class DepartmentEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private OrganizationEntity organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_department_id")
    private DepartmentEntity parent;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    protected DepartmentEntity() {
    }

    public DepartmentEntity(UUID id, OrganizationEntity organization, DepartmentEntity parent, String name) {
        this.id = id;
        this.organization = organization;
        this.parent = parent;
        this.name = name;
    }

    public UUID getId() {
        return id;
    }

    public OrganizationEntity getOrganization() {
        return organization;
    }

    public DepartmentEntity getParent() {
        return parent;
    }

    public String getName() {
        return name;
    }

    public void update(DepartmentEntity parent, String name) {
        this.parent = parent;
        this.name = name;
    }
}
