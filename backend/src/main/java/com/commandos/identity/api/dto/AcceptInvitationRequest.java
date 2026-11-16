package com.commandos.identity.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AcceptInvitationRequest(
        @NotBlank @Size(max = 100) String token,
        @NotBlank @Size(max = 160) String displayName,
        @NotBlank @Size(min = 12, max = 72) String password) {
}
