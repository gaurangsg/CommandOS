package com.commandos.people.domain;

import com.commandos.identity.domain.UserEntity;
import com.commandos.organization.domain.DepartmentEntity;
import com.commandos.organization.domain.OrganizationEntity;
import com.commandos.organization.domain.RoleEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "employee_invitations")
public class InvitationEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private OrganizationEntity organization;

    @Column(name = "invited_email", nullable = false, length = 254)
    private String invitedEmail;

    @Column(name = "display_name", nullable = false, length = 160)
    private String displayName;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private RoleEntity role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private DepartmentEntity department;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invited_by_user_id", nullable = false)
    private UserEntity invitedBy;

    @Column(name = "token_hash", nullable = false, unique = true, columnDefinition = "BINARY(32)")
    @JdbcTypeCode(SqlTypes.BINARY)
    private byte[] tokenHash;

    @Column(nullable = false, length = 24)
    private String status;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    protected InvitationEntity() {
    }

    public InvitationEntity(
            UUID id,
            OrganizationEntity organization,
            String invitedEmail,
            String displayName,
            RoleEntity role,
            DepartmentEntity department,
            UserEntity invitedBy,
            byte[] tokenHash,
            Instant expiresAt) {
        this.id = id;
        this.organization = organization;
        this.invitedEmail = invitedEmail;
        this.displayName = displayName;
        this.role = role;
        this.department = department;
        this.invitedBy = invitedBy;
        this.tokenHash = tokenHash;
        this.status = "PENDING";
        this.expiresAt = expiresAt;
    }

    public UUID getId() {
        return id;
    }

    public OrganizationEntity getOrganization() {
        return organization;
    }

    public String getInvitedEmail() {
        return invitedEmail;
    }

    public String getDisplayName() {
        return displayName;
    }

    public RoleEntity getRole() {
        return role;
    }

    public DepartmentEntity getDepartment() {
        return department;
    }

    public String getStatus() {
        return status;
    }

    public byte[] getTokenHash() {
        return tokenHash.clone();
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void accept(Instant at) {
        this.status = "ACCEPTED";
        this.acceptedAt = at;
    }

    public void revoke(Instant at) {
        this.status = "REVOKED";
        this.revokedAt = at;
    }

    public void expire() {
        this.status = "EXPIRED";
    }
}
