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
class ReactivateCatalogItemServiceTest {

    private static final String TENANT_ID = "travesia-natural";

    @Mock
    private CatalogItemRepositoryPort catalogItemRepositoryPort;

    private ReactivateCatalogItemService reactivateCatalogItemService;

    @BeforeEach
    void setUp() {
        reactivateCatalogItemService = new ReactivateCatalogItemService(catalogItemRepositoryPort);
    }

    @Test
    void reactivatesACatalogItem() {
        UUID catalogItemId = UUID.randomUUID();
        CatalogItem inactive = CatalogItem.create(TENANT_ID, CatalogItemType.TOUR, "Laguna Verde",
                BigDecimal.valueOf(100), null, null, null, null, null, null, null, null).deactivate();
        when(catalogItemRepositoryPort.findByTenantIdAndCatalogItemId(TENANT_ID, catalogItemId))
                .thenReturn(Optional.of(inactive));
        when(catalogItemRepositoryPort.save(any(CatalogItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CatalogItem result = reactivateCatalogItemService.reactivateCatalogItem(TENANT_ID, catalogItemId);

        assertThat(result.active()).isTrue();
    }

    @Test
    void rejectsWhenCatalogItemDoesNotExist() {
        UUID catalogItemId = UUID.randomUUID();
        when(catalogItemRepositoryPort.findByTenantIdAndCatalogItemId(TENANT_ID, catalogItemId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> reactivateCatalogItemService.reactivateCatalogItem(TENANT_ID, catalogItemId))
                .isInstanceOf(CatalogItemNotFoundException.class);
    }
}
