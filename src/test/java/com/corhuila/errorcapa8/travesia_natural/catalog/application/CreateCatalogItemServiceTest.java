package com.corhuila.errorcapa8.travesia_natural.catalog.application;

import com.corhuila.errorcapa8.travesia_natural.catalog.domain.model.CatalogItem;
import com.corhuila.errorcapa8.travesia_natural.catalog.domain.model.CatalogItemType;
import com.corhuila.errorcapa8.travesia_natural.catalog.domain.port.in.CreateCatalogItemCommand;
import com.corhuila.errorcapa8.travesia_natural.catalog.domain.port.out.CatalogItemRepositoryPort;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateCatalogItemServiceTest {

    private static final String TENANT_ID = "travesia-natural";

    @Mock
    private TenantRepositoryPort tenantRepositoryPort;
    @Mock
    private CatalogItemRepositoryPort catalogItemRepositoryPort;

    private CreateCatalogItemService createCatalogItemService;

    @BeforeEach
    void setUp() {
        createCatalogItemService = new CreateCatalogItemService(tenantRepositoryPort, catalogItemRepositoryPort);
    }

    private CreateCatalogItemCommand aCommand() {
        return new CreateCatalogItemCommand(TENANT_ID, CatalogItemType.TOUR, "Laguna Verde",
                BigDecimal.valueOf(100), null, null, null, null, null, null, null, null);
    }

    @Test
    void createsACatalogItem() {
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(Tenant.create(TENANT_ID, "Travesia")));
        when(catalogItemRepositoryPort.save(any(CatalogItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CatalogItem result = createCatalogItemService.createCatalogItem(aCommand());

        assertThat(result.name()).isEqualTo("Laguna Verde");
    }

    @Test
    void rejectsWhenTenantDoesNotExist() {
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> createCatalogItemService.createCatalogItem(aCommand()))
                .isInstanceOf(TenantNotFoundException.class);
    }

    @Test
    void rejectsWhenTenantIsInactive() {
        Tenant inactiveTenant = Tenant.create(TENANT_ID, "Travesia").deactivate();
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(inactiveTenant));

        assertThatThrownBy(() -> createCatalogItemService.createCatalogItem(aCommand()))
                .isInstanceOf(TenantInactiveException.class);
    }
}
