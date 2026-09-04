package com.corhuila.errorcapa8.travesia_natural.catalog.domain.port.in;

import com.corhuila.errorcapa8.travesia_natural.catalog.domain.model.CatalogItem;

import java.util.UUID;

public interface ReactivateCatalogItemUseCase {

    CatalogItem reactivateCatalogItem(String tenantId, UUID catalogItemId);
}
