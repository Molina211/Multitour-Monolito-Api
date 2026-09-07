package com.corhuila.errorcapa8.travesia_natural.catalog.domain.port.out;

import com.corhuila.errorcapa8.travesia_natural.catalog.domain.model.CatalogItem;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CatalogItemRepositoryPort {

    CatalogItem save(CatalogItem catalogItem);

    Optional<CatalogItem> findByTenantIdAndCatalogItemId(String tenantId, UUID catalogItemId);

    List<CatalogItem> findAllByTenantId(String tenantId);
}
