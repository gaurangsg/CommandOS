package com.commandos.identity.domain;

import com.commandos.organization.domain.OrganizationEntity;
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
@Table(name = "refresh_tokens")
public class RefreshTokenEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "family_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID familyId;

    @Column(name = "token_hash", nullable = false, unique = true, columnDefinition = "BINARY(32)")
    @JdbcTypeCode(SqlTypes.BINARY)
    private byte[] tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replaced_by_token_id")
    private RefreshTokenEntity replacedBy;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    protected RefreshTokenEntity() {
    }

    public RefreshTokenEntity(UUID id, UUID familyId, UserEntity user, byte[] tokenHash, Instant expiresAt) {
        this.id = id;
        this.familyId = familyId;
        this.user = user;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public UUID getFamilyId() {
        return familyId;
    }

    public UserEntity getUser() {
        return user;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public RefreshTokenEntity getReplacedBy() {
        return replacedBy;
    }

    public void revoke(Instant at, RefreshTokenEntity replacement) {
        this.revokedAt = at;
        this.replacedBy = replacement;
    }

    public void revoke(Instant at) {
        this.revokedAt = at;
    }
}
