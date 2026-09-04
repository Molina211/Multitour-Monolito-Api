package com.corhuila.errorcapa8.travesia_natural.discounts.application;

import com.corhuila.errorcapa8.travesia_natural.catalog.domain.exception.CatalogItemNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.catalog.domain.model.CatalogItem;
import com.corhuila.errorcapa8.travesia_natural.catalog.domain.port.out.CatalogItemRepositoryPort;
import com.corhuila.errorcapa8.travesia_natural.discounts.domain.model.Discount;
import com.corhuila.errorcapa8.travesia_natural.discounts.domain.port.in.CreateDiscountCommand;
import com.corhuila.errorcapa8.travesia_natural.discounts.domain.port.in.CreateDiscountUseCase;
import com.corhuila.errorcapa8.travesia_natural.discounts.domain.port.out.DiscountRepositoryPort;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantInactiveException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.Tenant;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.TenantStatus;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.out.TenantRepositoryPort;
import org.springframework.stereotype.Service;

@Service
public class CreateDiscountService implements CreateDiscountUseCase {

    private final TenantRepositoryPort tenantRepositoryPort;
    private final CatalogItemRepositoryPort catalogItemRepositoryPort;
    private final DiscountRepositoryPort discountRepositoryPort;

    public CreateDiscountService(TenantRepositoryPort tenantRepositoryPort,
                                  CatalogItemRepositoryPort catalogItemRepositoryPort,
                                  DiscountRepositoryPort discountRepositoryPort) {
        this.tenantRepositoryPort = tenantRepositoryPort;
        this.catalogItemRepositoryPort = catalogItemRepositoryPort;
        this.discountRepositoryPort = discountRepositoryPort;
    }

    @Override
    public Discount createDiscount(CreateDiscountCommand command) {
        Tenant tenant = tenantRepositoryPort.findById(command.tenantId())
                .orElseThrow(() -> new TenantNotFoundException(command.tenantId()));

        if (tenant.tenantStatus() == TenantStatus.INACTIVO) {
            throw new TenantInactiveException(tenant.tenantId());
        }

        CatalogItem catalogItem = catalogItemRepositoryPort
                .findByTenantIdAndCatalogItemId(tenant.tenantId(), command.catalogItemId())
                .orElseThrow(() -> new CatalogItemNotFoundException(command.catalogItemId().toString()));

        Discount discount = Discount.create(
                tenant.tenantId(), catalogItem.catalogItemId(), command.percentage(), command.validFrom(),
                command.validTo(), command.priority(), command.stackable(), command.cap(), command.base());

        return discountRepositoryPort.save(discount);
    }
}
