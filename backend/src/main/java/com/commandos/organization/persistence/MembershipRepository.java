package com.commandos.organization.persistence;

import com.commandos.organization.domain.MembershipEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;
import java.util.UUID;

public interface MembershipRepository extends JpaRepository<MembershipEntity, UUID> {

    @Query("""
            select distinct membership
            from MembershipEntity membership
            join fetch membership.user user
            join fetch membership.organization organization
            join fetch membership.role role
            left join fetch role.permissions
            where lower(user.email) = :email and lower(organization.slug) = :organizationSlug
            """)
    Optional<MembershipEntity> findForAuthentication(
            @Param("email") String email,
            @Param("organizationSlug") String organizationSlug);

    @Query("""
            select distinct membership
            from MembershipEntity membership
            join fetch membership.user user
            join fetch membership.organization organization
            join fetch membership.role role
            left join fetch role.permissions
            where user.id = :userId and lower(organization.slug) = :organizationSlug
            """)
    Optional<MembershipEntity> findForRefresh(
            @Param("userId") UUID userId,
            @Param("organizationSlug") String organizationSlug);

    Optional<MembershipEntity> findByOrganizationIdAndUserId(UUID organizationId, UUID userId);

    boolean existsByOrganizationIdAndUserIdAndStatus(UUID organizationId, UUID userId, String status);

    boolean existsByDepartmentId(UUID departmentId);
}
