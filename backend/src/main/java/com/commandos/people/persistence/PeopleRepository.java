package com.commandos.people.persistence;

import com.commandos.organization.domain.MembershipEntity;
import com.commandos.people.api.dto.EmployeeResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface PeopleRepository extends Repository<MembershipEntity, UUID> {

    @Query("""
            select new com.commandos.people.api.dto.EmployeeResponse(
                user.id, user.displayName, user.email, membership.status, role.name,
                membership.jobTitle, department.id, department.name,
                manager.id, manager.displayName, membership.employmentType, membership.startDate
            )
            from MembershipEntity membership
            join membership.user user
            join membership.role role
            left join membership.department department
            left join membership.manager manager
            where membership.organization.id = :organizationId
                and membership.status = 'ACTIVE'
                and user.status = 'ACTIVE'
                and (:departmentId is null or department.id = :departmentId)
                and (:search is null or
                    lower(user.displayName) like lower(concat('%', :search, '%')) or
                    lower(user.email) like lower(concat('%', :search, '%')))
            order by user.displayName asc
            """)
    Page<EmployeeResponse> searchEmployees(
            @Param("organizationId") UUID organizationId,
            @Param("departmentId") UUID departmentId,
            @Param("search") String search,
            Pageable pageable);
}
