package com.corhuila.errorcapa8.travesia_natural.discounts.application;

import com.corhuila.errorcapa8.travesia_natural.catalog.domain.exception.CatalogItemNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.catalog.domain.model.CatalogItem;
import com.corhuila.errorcapa8.travesia_natural.catalog.domain.model.CatalogItemType;
import com.corhuila.errorcapa8.travesia_natural.catalog.domain.port.out.CatalogItemRepositoryPort;
import com.corhuila.errorcapa8.travesia_natural.discounts.domain.model.Discount;
import com.corhuila.errorcapa8.travesia_natural.discounts.domain.model.DiscountBase;
import com.corhuila.errorcapa8.travesia_natural.discounts.domain.port.in.CreateDiscountCommand;
import com.corhuila.errorcapa8.travesia_natural.discounts.domain.port.out.DiscountRepositoryPort;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantInactiveException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.Tenant;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.out.TenantRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateDiscountServiceTest {

    private static final String TENANT_ID = "travesia-natural";

    @Mock
    private TenantRepositoryPort tenantRepositoryPort;
    @Mock
    private CatalogItemRepositoryPort catalogItemRepositoryPort;
    @Mock
    private DiscountRepositoryPort discountRepositoryPort;

    private CreateDiscountService createDiscountService;

    @BeforeEach
    void setUp() {
        createDiscountService = new CreateDiscountService(tenantRepositoryPort, catalogItemRepositoryPort,
                discountRepositoryPort);
    }

    private CatalogItem aCatalogItem() {
        return CatalogItem.create(TENANT_ID, CatalogItemType.TOUR, "Laguna Verde", BigDecimal.valueOf(100), null,
                null, null, null, null, null, null, null);
    }

    private CreateDiscountCommand aCommand(UUID catalogItemId) {
        return new CreateDiscountCommand(TENANT_ID, catalogItemId, 10, null, null, 1, false, null,
                DiscountBase.ORIGINAL_VALUE);
    }

    @Test
    void createsADiscount() {
        CatalogItem catalogItem = aCatalogItem();
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(Tenant.create(TENANT_ID, "Travesia")));
        when(catalogItemRepositoryPort.findByTenantIdAndCatalogItemId(TENANT_ID, catalogItem.catalogItemId()))
                .thenReturn(Optional.of(catalogItem));
        when(discountRepositoryPort.save(any(Discount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Discount result = createDiscountService.createDiscount(aCommand(catalogItem.catalogItemId()));

        assertThat(result.percentage()).isEqualTo(10);
    }

    @Test
    void rejectsWhenTenantDoesNotExist() {
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> createDiscountService.createDiscount(aCommand(UUID.randomUUID())))
                .isInstanceOf(TenantNotFoundException.class);
    }

    @Test
    void rejectsWhenTenantIsInactive() {
        Tenant inactiveTenant = Tenant.create(TENANT_ID, "Travesia").deactivate();
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(inactiveTenant));

        assertThatThrownBy(() -> createDiscountService.createDiscount(aCommand(UUID.randomUUID())))
                .isInstanceOf(TenantInactiveException.class);
    }

    @Test
    void rejectsWhenCatalogItemDoesNotExist() {
        UUID catalogItemId = UUID.randomUUID();
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(Tenant.create(TENANT_ID, "Travesia")));
        when(catalogItemRepositoryPort.findByTenantIdAndCatalogItemId(TENANT_ID, catalogItemId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> createDiscountService.createDiscount(aCommand(catalogItemId)))
                .isInstanceOf(CatalogItemNotFoundException.class);
    }
}
