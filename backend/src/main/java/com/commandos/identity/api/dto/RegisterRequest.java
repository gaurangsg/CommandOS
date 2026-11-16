package com.commandos.identity.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max = 160) String displayName,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 12, max = 72) String password,
        @NotBlank @Size(max = 160) String organizationName,
        @NotBlank
        @Size(min = 3, max = 63)
        @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$")
        String organizationSlug) {
}
