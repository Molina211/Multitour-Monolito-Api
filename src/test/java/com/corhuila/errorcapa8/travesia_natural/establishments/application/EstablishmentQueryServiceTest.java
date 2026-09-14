package com.corhuila.errorcapa8.travesia_natural.establishments.application;

import com.corhuila.errorcapa8.travesia_natural.establishments.domain.model.AssociatedEstablishment;
import com.corhuila.errorcapa8.travesia_natural.establishments.domain.model.EstablishmentKind;
import com.corhuila.errorcapa8.travesia_natural.establishments.domain.port.out.EstablishmentRepositoryPort;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.out.TenantRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EstablishmentQueryServiceTest {

    private static final String TENANT_ID = "travesia-natural";

    @Mock
    private TenantRepositoryPort tenantRepositoryPort;
    @Mock
    private EstablishmentRepositoryPort establishmentRepositoryPort;

    private EstablishmentQueryService establishmentQueryService;

    @BeforeEach
    void setUp() {
        establishmentQueryService = new EstablishmentQueryService(tenantRepositoryPort, establishmentRepositoryPort);
    }

    @Test
    void listsEstablishmentsOfATenant() {
        AssociatedEstablishment establishment = AssociatedEstablishment.create(TENANT_ID, EstablishmentKind.RESTAURANT,
                "Restaurante Sabor", null, null);
        when(tenantRepositoryPort.existsById(TENANT_ID)).thenReturn(true);
        when(establishmentRepositoryPort.findAllByTenantId(TENANT_ID)).thenReturn(List.of(establishment));

        assertThat(establishmentQueryService.listByTenant(TENANT_ID)).containsExactly(establishment);
    }

    @Test
    void rejectsWhenTenantDoesNotExist() {
        when(tenantRepositoryPort.existsById(TENANT_ID)).thenReturn(false);

        assertThatThrownBy(() -> establishmentQueryService.listByTenant(TENANT_ID))
                .isInstanceOf(TenantNotFoundException.class);
    }
}
