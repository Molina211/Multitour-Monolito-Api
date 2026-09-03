package com.corhuila.errorcapa8.travesia_natural.catalog.domain.port.in;

import com.corhuila.errorcapa8.travesia_natural.catalog.domain.model.CatalogItem;

public interface UpdateCatalogItemUseCase {

    CatalogItem updateCatalogItem(UpdateCatalogItemCommand command);
}
