package com.commandos.organization.persistence;

import com.commandos.organization.domain.OrganizationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrganizationRepository extends JpaRepository<OrganizationEntity, UUID> {

    boolean existsBySlug(String slug);
}
