package com.corhuila.errorcapa8.travesia_natural.catalog.domain.port.in;

import com.corhuila.errorcapa8.travesia_natural.catalog.domain.model.CatalogItem;

import java.util.List;
import java.util.UUID;

public interface CatalogItemQueryUseCase {

    CatalogItem getById(String tenantId, UUID catalogItemId);

    List<CatalogItem> listByTenant(String tenantId);
}
