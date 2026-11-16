package com.commandos.identity.api.dto;

import java.util.Set;
import java.util.UUID;

public record AuthUserResponse(
        UUID userId,
        String email,
        String displayName,
        UUID organizationId,
        String organizationName,
        String organizationSlug,
        String role,
        Set<String> permissions) {
}
