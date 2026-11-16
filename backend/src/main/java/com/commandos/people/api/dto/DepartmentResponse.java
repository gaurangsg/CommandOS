package com.commandos.people.api.dto;

import java.util.UUID;

public record DepartmentResponse(
        UUID id,
        String name,
        UUID parentDepartmentId,
        String parentDepartmentName,
        long memberCount) {
}
