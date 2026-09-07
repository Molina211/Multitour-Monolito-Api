package com.corhuila.errorcapa8.travesia_natural.establishments.application;

import com.corhuila.errorcapa8.travesia_natural.establishments.domain.model.AssociatedEstablishment;
import com.corhuila.errorcapa8.travesia_natural.establishments.domain.model.EstablishmentKind;
import com.corhuila.errorcapa8.travesia_natural.establishments.domain.port.in.CreateEstablishmentCommand;
import com.corhuila.errorcapa8.travesia_natural.establishments.domain.port.out.EstablishmentRepositoryPort;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantInactiveException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.Tenant;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.out.TenantRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateEstablishmentServiceTest {

    private static final String TENANT_ID = "travesia-natural";

    @Mock
    private TenantRepositoryPort tenantRepositoryPort;
    @Mock
    private EstablishmentRepositoryPort establishmentRepositoryPort;

    private CreateEstablishmentService createEstablishmentService;

    @BeforeEach
    void setUp() {
        createEstablishmentService = new CreateEstablishmentService(tenantRepositoryPort, establishmentRepositoryPort);
    }

    private CreateEstablishmentCommand aCommand() {
        return new CreateEstablishmentCommand(TENANT_ID, EstablishmentKind.HOTEL, "Hotel Andino", "Descripcion",
                "image.png");
    }

    @Test
    void createsAnEstablishment() {
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(Tenant.create(TENANT_ID, "Travesia")));
        when(establishmentRepositoryPort.save(any(AssociatedEstablishment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AssociatedEstablishment result = createEstablishmentService.createEstablishment(aCommand());

        assertThat(result.name()).isEqualTo("Hotel Andino");
        assertThat(result.kind()).isEqualTo(EstablishmentKind.HOTEL);
    }

    @Test
    void rejectsWhenTenantDoesNotExist() {
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> createEstablishmentService.createEstablishment(aCommand()))
                .isInstanceOf(TenantNotFoundException.class);
    }

    @Test
    void rejectsWhenTenantIsInactive() {
        Tenant inactiveTenant = Tenant.create(TENANT_ID, "Travesia").deactivate();
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(inactiveTenant));

        assertThatThrownBy(() -> createEstablishmentService.createEstablishment(aCommand()))
                .isInstanceOf(TenantInactiveException.class);
    }
}
