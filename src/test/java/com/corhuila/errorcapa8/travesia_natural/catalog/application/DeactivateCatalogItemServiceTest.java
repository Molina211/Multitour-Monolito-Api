package com.corhuila.errorcapa8.travesia_natural.catalog.application;

import com.corhuila.errorcapa8.travesia_natural.catalog.domain.exception.CatalogItemNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.catalog.domain.model.CatalogItem;
import com.corhuila.errorcapa8.travesia_natural.catalog.domain.model.CatalogItemType;
import com.corhuila.errorcapa8.travesia_natural.catalog.domain.port.out.CatalogItemRepositoryPort;
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
class DeactivateCatalogItemServiceTest {

    private static final String TENANT_ID = "travesia-natural";

    @Mock
    private CatalogItemRepositoryPort catalogItemRepositoryPort;

    private DeactivateCatalogItemService deactivateCatalogItemService;

    @BeforeEach
    void setUp() {
        deactivateCatalogItemService = new DeactivateCatalogItemService(catalogItemRepositoryPort);
    }

    @Test
    void deactivatesACatalogItem() {
        UUID catalogItemId = UUID.randomUUID();
        CatalogItem item = CatalogItem.create(TENANT_ID, CatalogItemType.TOUR, "Laguna Verde",
                BigDecimal.valueOf(100), null, null, null, null, null, null, null, null);
        when(catalogItemRepositoryPort.findByTenantIdAndCatalogItemId(TENANT_ID, catalogItemId))
                .thenReturn(Optional.of(item));
        when(catalogItemRepositoryPort.save(any(CatalogItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CatalogItem result = deactivateCatalogItemService.deactivateCatalogItem(TENANT_ID, catalogItemId);

        assertThat(result.active()).isFalse();
    }

    @Test
    void rejectsWhenCatalogItemDoesNotExist() {
        UUID catalogItemId = UUID.randomUUID();
        when(catalogItemRepositoryPort.findByTenantIdAndCatalogItemId(TENANT_ID, catalogItemId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> deactivateCatalogItemService.deactivateCatalogItem(TENANT_ID, catalogItemId))
                .isInstanceOf(CatalogItemNotFoundException.class);
    }
}
