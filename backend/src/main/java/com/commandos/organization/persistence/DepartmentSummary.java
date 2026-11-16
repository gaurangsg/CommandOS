package com.commandos.organization.persistence;

import java.util.UUID;

public interface DepartmentSummary {

    UUID getId();

    String getName();

    UUID getParentDepartmentId();

    String getParentDepartmentName();

    long getMemberCount();
}
