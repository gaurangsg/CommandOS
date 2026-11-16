package com.commandos.people.application;

import com.commandos.identity.domain.UserEntity;
import com.commandos.organization.domain.DepartmentEntity;
import com.commandos.organization.domain.MembershipEntity;
import com.commandos.organization.persistence.DepartmentRepository;
import com.commandos.organization.persistence.MembershipRepository;
import com.commandos.people.api.dto.EmployeeProfileRequest;
import com.commandos.people.api.dto.EmployeeResponse;
import com.commandos.people.api.dto.PageResponse;
import com.commandos.people.persistence.PeopleRepository;
import com.commandos.shared.api.ApiException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PeopleService {

    private final MembershipRepository membershipRepository;
    private final DepartmentRepository departmentRepository;
    private final PeopleRepository peopleRepository;

    public PeopleService(
            MembershipRepository membershipRepository,
            DepartmentRepository departmentRepository,
            PeopleRepository peopleRepository) {
        this.membershipRepository = membershipRepository;
        this.departmentRepository = departmentRepository;
        this.peopleRepository = peopleRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<EmployeeResponse> listEmployees(
            UUID organizationId,
            int page,
            int size,
            String search,
            UUID departmentId) {
        String normalizedSearch = search == null || search.isBlank() ? null : search.trim();
        return PageResponse.from(peopleRepository.searchEmployees(
                organizationId,
                departmentId,
                normalizedSearch,
                PageRequest.of(page, size)));
    }

    @Transactional
    public EmployeeResponse updateProfile(
            UUID organizationId,
            UUID userId,
            EmployeeProfileRequest request) {
        MembershipEntity membership = membershipRepository.findByOrganizationIdAndUserId(organizationId, userId)
                .filter(candidate -> "ACTIVE".equals(candidate.getStatus())
                        && "ACTIVE".equals(candidate.getUser().getStatus()))
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "EMPLOYEE_NOT_FOUND", "The employee was not found in this workspace."));

        DepartmentEntity department = request.departmentId() == null
                ? null
                : departmentRepository.findByIdAndOrganizationId(request.departmentId(), organizationId)
                        .orElseThrow(() -> new ApiException(
                                HttpStatus.BAD_REQUEST,
                                "INVALID_DEPARTMENT",
                                "The department must belong to this workspace."));

        UserEntity manager = request.managerUserId() == null
                ? null
                : membershipRepository.findByOrganizationIdAndUserId(organizationId, request.managerUserId())
                        .filter(candidate -> "ACTIVE".equals(candidate.getStatus())
                                && "ACTIVE".equals(candidate.getUser().getStatus()))
                        .map(MembershipEntity::getUser)
                        .orElseThrow(() -> new ApiException(
                                HttpStatus.BAD_REQUEST,
                                "INVALID_MANAGER",
                                "The manager must be an active employee in this workspace."));
        if (userId.equals(request.managerUserId())) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "INVALID_MANAGER", "An employee cannot be their own manager.");
        }

        membership.updateEmployeeProfile(
                department,
                normalize(request.jobTitle()),
                manager,
                request.employmentType(),
                request.startDate());
        return toResponse(membership);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static EmployeeResponse toResponse(MembershipEntity membership) {
        return new EmployeeResponse(
                membership.getUser().getId(),
                membership.getUser().getDisplayName(),
                membership.getUser().getEmail(),
                membership.getStatus(),
                membership.getRole().getName(),
                membership.getJobTitle(),
                membership.getDepartment() == null ? null : membership.getDepartment().getId(),
                membership.getDepartment() == null ? null : membership.getDepartment().getName(),
                membership.getManager() == null ? null : membership.getManager().getId(),
                membership.getManager() == null ? null : membership.getManager().getDisplayName(),
                membership.getEmploymentType(),
                membership.getStartDate());
    }
}
