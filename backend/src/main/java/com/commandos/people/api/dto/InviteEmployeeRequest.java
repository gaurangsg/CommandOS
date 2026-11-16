package com.commandos.people.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record InviteEmployeeRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(max = 160) String displayName,
        UUID departmentId) {
}
