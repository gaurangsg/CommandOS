package com.commandos.people.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record DepartmentRequest(
        @NotBlank @Size(max = 120) String name,
        UUID parentDepartmentId) {
}
