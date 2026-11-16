package com.commandos.organization.persistence;

import com.commandos.organization.domain.DepartmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DepartmentRepository extends JpaRepository<DepartmentEntity, UUID> {

    @Query("""
            select department.id as id,
                department.name as name,
                parent.id as parentDepartmentId,
                parent.name as parentDepartmentName,
                count(member.id) as memberCount
            from DepartmentEntity department
            left join department.parent parent
            left join MembershipEntity member
                on member.department = department
                and member.organization = department.organization
                and member.status = 'ACTIVE'
                and member.user.status = 'ACTIVE'
            where department.organization.id = :organizationId
            group by department.id, department.name, parent.id, parent.name
            order by department.name asc
            """)
    List<DepartmentSummary> findDepartmentSummaries(@Param("organizationId") UUID organizationId);

    List<DepartmentEntity> findAllByOrganizationIdOrderByNameAsc(UUID organizationId);

    Optional<DepartmentEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    boolean existsByOrganizationIdAndNameIgnoreCase(UUID organizationId, String name);

    boolean existsByOrganizationIdAndNameIgnoreCaseAndIdNot(UUID organizationId, String name, UUID id);

    boolean existsByOrganizationIdAndParentId(UUID organizationId, UUID parentId);
}
