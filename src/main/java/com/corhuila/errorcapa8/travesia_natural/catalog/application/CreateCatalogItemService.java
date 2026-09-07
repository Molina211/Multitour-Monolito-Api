package com.corhuila.errorcapa8.travesia_natural.catalog.application;

import com.corhuila.errorcapa8.travesia_natural.catalog.domain.model.CatalogItem;
import com.corhuila.errorcapa8.travesia_natural.catalog.domain.port.in.CreateCatalogItemCommand;
import com.corhuila.errorcapa8.travesia_natural.catalog.domain.port.in.CreateCatalogItemUseCase;
import com.corhuila.errorcapa8.travesia_natural.catalog.domain.port.out.CatalogItemRepositoryPort;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantInactiveException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.Tenant;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.TenantStatus;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.out.TenantRepositoryPort;
import org.springframework.stereotype.Service;

@Service
public class CreateCatalogItemService implements CreateCatalogItemUseCase {

    private final TenantRepositoryPort tenantRepositoryPort;
    private final CatalogItemRepositoryPort catalogItemRepositoryPort;

    public CreateCatalogItemService(TenantRepositoryPort tenantRepositoryPort,
                                     CatalogItemRepositoryPort catalogItemRepositoryPort) {
        this.tenantRepositoryPort = tenantRepositoryPort;
        this.catalogItemRepositoryPort = catalogItemRepositoryPort;
    }

    @Override
    public CatalogItem createCatalogItem(CreateCatalogItemCommand command) {
        Tenant tenant = tenantRepositoryPort.findById(command.tenantId())
                .orElseThrow(() -> new TenantNotFoundException(command.tenantId()));

        if (tenant.tenantStatus() == TenantStatus.INACTIVO) {
            throw new TenantInactiveException(tenant.tenantId());
        }

        CatalogItem catalogItem = CatalogItem.create(
                tenant.tenantId(), command.type(), command.name(), command.price(), command.capacity(),
                command.restrictions(), command.validFrom(), command.validTo(), command.policy(), command.image(),
                command.route(), command.operationalCost());

        return catalogItemRepositoryPort.save(catalogItem);
    }
}
