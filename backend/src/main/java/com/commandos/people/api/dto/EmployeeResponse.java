package com.commandos.people.api.dto;

import java.time.LocalDate;
import java.util.UUID;

public record EmployeeResponse(
        UUID userId,
        String displayName,
        String email,
        String membershipStatus,
        String role,
        String jobTitle,
        UUID departmentId,
        String departmentName,
        UUID managerUserId,
        String managerName,
        String employmentType,
        LocalDate startDate) {
}
