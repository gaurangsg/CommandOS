package com.commandos.people.api.dto;

import java.time.Instant;

public record InvitationPreviewResponse(
        String email,
        String displayName,
        String organizationName,
        String organizationSlug,
        String departmentName,
        Instant expiresAt) {
}
