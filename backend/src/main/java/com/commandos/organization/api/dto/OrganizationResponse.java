package com.commandos.organization.api.dto;

import java.util.UUID;

public record OrganizationResponse(UUID id, String name, String slug, String role) {
}
