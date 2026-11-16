package com.commandos.people.application;

import com.commandos.organization.domain.MembershipEntity;
import com.commandos.organization.persistence.DepartmentRepository;
import com.commandos.organization.persistence.MembershipRepository;
import com.commandos.organization.persistence.OrganizationRepository;
import com.commandos.people.api.dto.DepartmentRequest;
import com.commandos.people.api.dto.EmployeeProfileRequest;
import com.commandos.people.persistence.PeopleRepository;
import com.commandos.shared.api.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PeopleServiceTest {

    private DepartmentRepository departmentRepository;
    private MembershipRepository membershipRepository;
    private DepartmentService departmentService;
    private PeopleService peopleService;

    @BeforeEach
    void setUp() {
        departmentRepository = mock(DepartmentRepository.class);
        membershipRepository = mock(MembershipRepository.class);
        departmentService = new DepartmentService(
                departmentRepository,
                mock(OrganizationRepository.class),
                membershipRepository);
        peopleService = new PeopleService(
                membershipRepository,
                departmentRepository,
                mock(PeopleRepository.class));
    }

    @Test
    void departmentCannotBeNestedUnderAnotherOrganization() {
        UUID organizationId = UUID.randomUUID();
        UUID foreignParentId = UUID.randomUUID();
        when(departmentRepository.findByIdAndOrganizationId(foreignParentId, organizationId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> departmentService.create(
                organizationId, new DepartmentRequest("Research", foreignParentId)))
                .isInstanceOf(ApiException.class)
                .satisfies(exception ->
                        org.assertj.core.api.Assertions.assertThat(((ApiException) exception).getStatus())
                                .isEqualTo(HttpStatus.BAD_REQUEST));

        verify(departmentRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void employeeCannotBeAssignedToAnotherOrganizationsDepartment() {
        UUID organizationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID foreignDepartmentId = UUID.randomUUID();
        MembershipEntity membership = mock(MembershipEntity.class);
        com.commandos.identity.domain.UserEntity user = mock(com.commandos.identity.domain.UserEntity.class);
        when(membership.getStatus()).thenReturn("ACTIVE");
        when(membership.getUser()).thenReturn(user);
        when(user.getStatus()).thenReturn("ACTIVE");
        when(membershipRepository.findByOrganizationIdAndUserId(organizationId, userId))
                .thenReturn(Optional.of(membership));
        when(departmentRepository.findByIdAndOrganizationId(foreignDepartmentId, organizationId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> peopleService.updateProfile(
                organizationId,
                userId,
                new EmployeeProfileRequest("Designer", foreignDepartmentId, null, "FULL_TIME", null)))
                .isInstanceOf(ApiException.class)
                .satisfies(exception ->
                        org.assertj.core.api.Assertions.assertThat(((ApiException) exception).getStatus())
                                .isEqualTo(HttpStatus.BAD_REQUEST));

        verify(membership, never()).updateEmployeeProfile(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }
}
