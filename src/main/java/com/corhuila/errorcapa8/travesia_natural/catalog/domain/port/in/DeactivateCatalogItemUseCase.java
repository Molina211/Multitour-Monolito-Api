package com.corhuila.errorcapa8.travesia_natural.catalog.domain.port.in;

import com.corhuila.errorcapa8.travesia_natural.catalog.domain.model.CatalogItem;

import java.util.UUID;

public interface DeactivateCatalogItemUseCase {

    CatalogItem deactivateCatalogItem(String tenantId, UUID catalogItemId);
}
