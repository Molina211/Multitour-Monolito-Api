package com.corhuila.errorcapa8.travesia_natural.catalog.application;

import com.corhuila.errorcapa8.travesia_natural.catalog.domain.exception.CatalogItemNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.catalog.domain.model.CatalogItem;
import com.corhuila.errorcapa8.travesia_natural.catalog.domain.port.in.UpdateCatalogItemCommand;
import com.corhuila.errorcapa8.travesia_natural.catalog.domain.port.in.UpdateCatalogItemUseCase;
import com.corhuila.errorcapa8.travesia_natural.catalog.domain.port.out.CatalogItemRepositoryPort;
import org.springframework.stereotype.Service;

@Service
public class UpdateCatalogItemService implements UpdateCatalogItemUseCase {

    private final CatalogItemRepositoryPort catalogItemRepositoryPort;

    public UpdateCatalogItemService(CatalogItemRepositoryPort catalogItemRepositoryPort) {
        this.catalogItemRepositoryPort = catalogItemRepositoryPort;
    }

    @Override
    public CatalogItem updateCatalogItem(UpdateCatalogItemCommand command) {
        CatalogItem catalogItem = catalogItemRepositoryPort
                .findByTenantIdAndCatalogItemId(command.tenantId(), command.catalogItemId())
                .orElseThrow(() -> new CatalogItemNotFoundException(command.catalogItemId().toString()));

        CatalogItem updated = catalogItem.update(
                command.name(), command.price(), command.capacity(), command.restrictions(), command.validFrom(),
                command.validTo(), command.policy(), command.image(), command.route(), command.operationalCost());

        return catalogItemRepositoryPort.save(updated);
    }
}
