package com.corhuila.errorcapa8.travesia_natural.catalog.application;

import com.corhuila.errorcapa8.travesia_natural.catalog.domain.exception.CatalogItemNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.catalog.domain.model.CatalogItem;
import com.corhuila.errorcapa8.travesia_natural.catalog.domain.model.CatalogItemType;
import com.corhuila.errorcapa8.travesia_natural.catalog.domain.port.out.CatalogItemRepositoryPort;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.out.TenantRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogItemQueryServiceTest {

    private static final String TENANT_ID = "travesia-natural";

    @Mock
    private TenantRepositoryPort tenantRepositoryPort;
    @Mock
    private CatalogItemRepositoryPort catalogItemRepositoryPort;

    private CatalogItemQueryService catalogItemQueryService;

    @BeforeEach
    void setUp() {
        catalogItemQueryService = new CatalogItemQueryService(tenantRepositoryPort, catalogItemRepositoryPort);
    }

    private CatalogItem aTour() {
        return CatalogItem.create(TENANT_ID, CatalogItemType.TOUR, "Laguna Verde", BigDecimal.valueOf(100), null,
                null, null, null, null, null, null, null);
    }

    @Test
    void getsACatalogItemById() {
        UUID catalogItemId = UUID.randomUUID();
        CatalogItem item = aTour();
        when(tenantRepositoryPort.existsById(TENANT_ID)).thenReturn(true);
        when(catalogItemRepositoryPort.findByTenantIdAndCatalogItemId(TENANT_ID, catalogItemId))
                .thenReturn(Optional.of(item));

        assertThat(catalogItemQueryService.getById(TENANT_ID, catalogItemId)).isEqualTo(item);
    }

    @Test
    void rejectsWhenTenantDoesNotExist() {
        UUID catalogItemId = UUID.randomUUID();
        when(tenantRepositoryPort.existsById(TENANT_ID)).thenReturn(false);

        assertThatThrownBy(() -> catalogItemQueryService.getById(TENANT_ID, catalogItemId))
                .isInstanceOf(TenantNotFoundException.class);
    }

    @Test
    void rejectsWhenCatalogItemDoesNotExist() {
        UUID catalogItemId = UUID.randomUUID();
        when(tenantRepositoryPort.existsById(TENANT_ID)).thenReturn(true);
        when(catalogItemRepositoryPort.findByTenantIdAndCatalogItemId(TENANT_ID, catalogItemId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> catalogItemQueryService.getById(TENANT_ID, catalogItemId))
                .isInstanceOf(CatalogItemNotFoundException.class);
    }

    @Test
    void listsCatalogItemsOfATenant() {
        CatalogItem item = aTour();
        when(tenantRepositoryPort.existsById(TENANT_ID)).thenReturn(true);
        when(catalogItemRepositoryPort.findAllByTenantId(TENANT_ID)).thenReturn(List.of(item));

        assertThat(catalogItemQueryService.listByTenant(TENANT_ID)).containsExactly(item);
    }
}
