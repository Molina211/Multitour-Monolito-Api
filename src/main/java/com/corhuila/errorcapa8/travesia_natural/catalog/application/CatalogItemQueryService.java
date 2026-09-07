package com.corhuila.errorcapa8.travesia_natural.catalog.application;

import com.corhuila.errorcapa8.travesia_natural.catalog.domain.exception.CatalogItemNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.catalog.domain.model.CatalogItem;
import com.corhuila.errorcapa8.travesia_natural.catalog.domain.port.in.CatalogItemQueryUseCase;
import com.corhuila.errorcapa8.travesia_natural.catalog.domain.port.out.CatalogItemRepositoryPort;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.out.TenantRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class CatalogItemQueryService implements CatalogItemQueryUseCase {

    private final TenantRepositoryPort tenantRepositoryPort;
    private final CatalogItemRepositoryPort catalogItemRepositoryPort;

    public CatalogItemQueryService(TenantRepositoryPort tenantRepositoryPort,
                                    CatalogItemRepositoryPort catalogItemRepositoryPort) {
        this.tenantRepositoryPort = tenantRepositoryPort;
        this.catalogItemRepositoryPort = catalogItemRepositoryPort;
    }

    @Override
    public CatalogItem getById(String tenantId, UUID catalogItemId) {
        requireTenant(tenantId);

        return catalogItemRepositoryPort.findByTenantIdAndCatalogItemId(tenantId, catalogItemId)
                .orElseThrow(() -> new CatalogItemNotFoundException(catalogItemId.toString()));
    }

    @Override
    public List<CatalogItem> listByTenant(String tenantId) {
        requireTenant(tenantId);

        return catalogItemRepositoryPort.findAllByTenantId(tenantId);
    }

    private void requireTenant(String tenantId) {
        if (!tenantRepositoryPort.existsById(tenantId)) {
            throw new TenantNotFoundException(tenantId);
        }
    }
}
