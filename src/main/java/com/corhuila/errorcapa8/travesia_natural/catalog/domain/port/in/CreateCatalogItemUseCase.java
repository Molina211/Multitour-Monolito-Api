package com.corhuila.errorcapa8.travesia_natural.catalog.domain.port.in;

import com.corhuila.errorcapa8.travesia_natural.catalog.domain.model.CatalogItem;

public interface CreateCatalogItemUseCase {

    CatalogItem createCatalogItem(CreateCatalogItemCommand command);
}
