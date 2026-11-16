package com.commandos.people.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record EmployeeProfileRequest(
        @Size(max = 120) String jobTitle,
        UUID departmentId,
        UUID managerUserId,
        @NotNull @Pattern(regexp = "FULL_TIME|PART_TIME|CONTRACTOR|INTERN") String employmentType,
        LocalDate startDate) {
}
