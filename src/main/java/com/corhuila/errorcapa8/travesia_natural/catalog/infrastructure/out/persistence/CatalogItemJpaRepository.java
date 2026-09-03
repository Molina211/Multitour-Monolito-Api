package com.corhuila.errorcapa8.travesia_natural.catalog.infrastructure.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CatalogItemJpaRepository extends JpaRepository<CatalogItemEntity, UUID> {

    Optional<CatalogItemEntity> findByTenantIdAndCatalogItemId(String tenantId, UUID catalogItemId);

    List<CatalogItemEntity> findAllByTenantId(String tenantId);
}
