package com.corhuila.errorcapa8.travesia_natural.catalog.application;

import com.corhuila.errorcapa8.travesia_natural.catalog.domain.exception.CatalogItemNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.catalog.domain.model.CatalogItem;
import com.corhuila.errorcapa8.travesia_natural.catalog.domain.port.in.ReactivateCatalogItemUseCase;
import com.corhuila.errorcapa8.travesia_natural.catalog.domain.port.out.CatalogItemRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ReactivateCatalogItemService implements ReactivateCatalogItemUseCase {

    private final CatalogItemRepositoryPort catalogItemRepositoryPort;

    public ReactivateCatalogItemService(CatalogItemRepositoryPort catalogItemRepositoryPort) {
        this.catalogItemRepositoryPort = catalogItemRepositoryPort;
    }

    @Override
    public CatalogItem reactivateCatalogItem(String tenantId, UUID catalogItemId) {
        CatalogItem catalogItem = catalogItemRepositoryPort.findByTenantIdAndCatalogItemId(tenantId, catalogItemId)
                .orElseThrow(() -> new CatalogItemNotFoundException(catalogItemId.toString()));

        return catalogItemRepositoryPort.save(catalogItem.reactivate());
    }
}
