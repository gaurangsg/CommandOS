package com.commandos.people.application;

import com.commandos.organization.domain.DepartmentEntity;
import com.commandos.organization.persistence.DepartmentRepository;
import com.commandos.organization.persistence.DepartmentSummary;
import com.commandos.organization.persistence.MembershipRepository;
import com.commandos.organization.persistence.OrganizationRepository;
import com.commandos.people.api.dto.DepartmentRequest;
import com.commandos.people.api.dto.DepartmentResponse;
import com.commandos.shared.api.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final OrganizationRepository organizationRepository;
    private final MembershipRepository membershipRepository;

    public DepartmentService(
            DepartmentRepository departmentRepository,
            OrganizationRepository organizationRepository,
            MembershipRepository membershipRepository) {
        this.departmentRepository = departmentRepository;
        this.organizationRepository = organizationRepository;
        this.membershipRepository = membershipRepository;
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> list(UUID organizationId) {
        return departmentRepository.findDepartmentSummaries(organizationId).stream()
                .map(DepartmentService::toResponse)
                .toList();
    }

    @Transactional
    public DepartmentResponse create(UUID organizationId, DepartmentRequest request) {
        String name = request.name().trim();
        ensureUniqueName(organizationId, name, null);
        DepartmentEntity parent = resolveParent(organizationId, request.parentDepartmentId());
        DepartmentEntity department = departmentRepository.save(new DepartmentEntity(
                UUID.randomUUID(),
                organizationRepository.getReferenceById(organizationId),
                parent,
                name));
        return findResponse(organizationId, department.getId());
    }

    @Transactional
    public DepartmentResponse update(UUID organizationId, UUID departmentId, DepartmentRequest request) {
        DepartmentEntity department = findDepartment(organizationId, departmentId);
        String name = request.name().trim();
        ensureUniqueName(organizationId, name, departmentId);
        DepartmentEntity parent = resolveParent(organizationId, request.parentDepartmentId());
        ensureNoCycle(department, parent);
        department.update(parent, name);
        return findResponse(organizationId, departmentId);
    }

    @Transactional
    public void delete(UUID organizationId, UUID departmentId) {
        DepartmentEntity department = findDepartment(organizationId, departmentId);
        if (departmentRepository.existsByOrganizationIdAndParentId(organizationId, departmentId)
                || membershipRepository.existsByDepartmentId(departmentId)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "DEPARTMENT_IN_USE",
                    "Move employees and child departments before deleting this department.");
        }
        departmentRepository.delete(department);
    }

    private DepartmentEntity findDepartment(UUID organizationId, UUID departmentId) {
        return departmentRepository.findByIdAndOrganizationId(departmentId, organizationId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "DEPARTMENT_NOT_FOUND", "The department was not found."));
    }

    private DepartmentEntity resolveParent(UUID organizationId, UUID parentDepartmentId) {
        if (parentDepartmentId == null) {
            return null;
        }
        return departmentRepository.findByIdAndOrganizationId(parentDepartmentId, organizationId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "INVALID_PARENT_DEPARTMENT",
                        "The parent department must belong to this workspace."));
    }

    private void ensureUniqueName(UUID organizationId, String name, UUID excludedId) {
        boolean exists = excludedId == null
                ? departmentRepository.existsByOrganizationIdAndNameIgnoreCase(organizationId, name)
                : departmentRepository.existsByOrganizationIdAndNameIgnoreCaseAndIdNot(
                        organizationId, name, excludedId);
        if (exists) {
            throw new ApiException(
                    HttpStatus.CONFLICT, "DEPARTMENT_NAME_EXISTS", "A department with this name already exists.");
        }
    }

    private static void ensureNoCycle(DepartmentEntity department, DepartmentEntity parent) {
        DepartmentEntity cursor = parent;
        while (cursor != null) {
            if (cursor.getId().equals(department.getId())) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "INVALID_DEPARTMENT_HIERARCHY",
                        "A department cannot be nested beneath itself or one of its descendants.");
            }
            cursor = cursor.getParent();
        }
    }

    private DepartmentResponse findResponse(UUID organizationId, UUID departmentId) {
        return departmentRepository.findDepartmentSummaries(organizationId).stream()
                .filter(department -> department.getId().equals(departmentId))
                .map(DepartmentService::toResponse)
                .findFirst()
                .orElseThrow(() -> new ApiException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "DEPARTMENT_RESPONSE_MISSING",
                        "The department could not be reloaded after saving."));
    }

    private static DepartmentResponse toResponse(DepartmentSummary department) {
        return new DepartmentResponse(
                department.getId(),
                department.getName(),
                department.getParentDepartmentId(),
                department.getParentDepartmentName(),
                department.getMemberCount());
    }
}
