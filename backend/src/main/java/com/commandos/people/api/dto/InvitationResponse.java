package com.commandos.people.api.dto;

import java.time.Instant;
import java.util.UUID;

public record InvitationResponse(
        UUID id,
        String email,
        String displayName,
        String status,
        UUID departmentId,
        Instant expiresAt) {
}
