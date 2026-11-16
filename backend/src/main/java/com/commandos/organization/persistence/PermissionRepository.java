package com.commandos.organization.persistence;

import com.commandos.organization.domain.PermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PermissionRepository extends JpaRepository<PermissionEntity, UUID> {

    List<PermissionEntity> findAllByPermissionKeyIn(Collection<String> permissionKeys);
}
