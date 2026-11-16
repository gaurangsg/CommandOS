package com.commandos.people.persistence;

import com.commandos.people.domain.InvitationEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvitationRepository extends JpaRepository<InvitationEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select invitation
            from InvitationEntity invitation
            join fetch invitation.organization
            join fetch invitation.role
            left join fetch invitation.department
            where invitation.tokenHash = :tokenHash
            """)
    Optional<InvitationEntity> findByTokenHashForUpdate(@Param("tokenHash") byte[] tokenHash);

    @Query("""
            select invitation
            from InvitationEntity invitation
            join fetch invitation.organization
            left join fetch invitation.department
            where invitation.tokenHash = :tokenHash
            """)
    Optional<InvitationEntity> findByTokenHash(@Param("tokenHash") byte[] tokenHash);

    @Modifying
    @Query("""
            update InvitationEntity invitation
            set invitation.status = 'REVOKED', invitation.revokedAt = :now
            where invitation.organization.id = :organizationId
                and lower(invitation.invitedEmail) = :email
                and invitation.status = 'PENDING'
            """)
    int revokePending(
            @Param("organizationId") UUID organizationId,
            @Param("email") String email,
            @Param("now") Instant now);

    List<InvitationEntity> findAllByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    Optional<InvitationEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
