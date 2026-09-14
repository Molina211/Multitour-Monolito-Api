package com.corhuila.errorcapa8.travesia_natural.reservations.application;

import com.corhuila.errorcapa8.travesia_natural.catalog.domain.model.CatalogItem;
import com.corhuila.errorcapa8.travesia_natural.catalog.domain.model.CatalogItemType;
import com.corhuila.errorcapa8.travesia_natural.catalog.domain.port.out.CatalogItemRepositoryPort;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.exception.InvalidReservationException;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.Reservation;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.ReservedService;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.in.CreateReservationCommand;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.out.ReservationRepositoryPort;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateReservationServiceTest {

    private static final String TENANT_ID = "travesia-natural";

    @Mock
    private TenantRepositoryPort tenantRepositoryPort;
    @Mock
    private ReservationRepositoryPort reservationRepositoryPort;
    @Mock
    private CatalogItemRepositoryPort catalogItemRepositoryPort;

    private CreateReservationService createReservationService;

    @BeforeEach
    void setUp() {
        createReservationService = new CreateReservationService(tenantRepositoryPort, reservationRepositoryPort,
                catalogItemRepositoryPort);
    }

    private ReservedService aReservedService() {
        return new ReservedService("tour-laguna", 2, LocalDate.now().plusDays(5), null, null);
    }

    private CreateReservationCommand aCommand(List<ReservedService> reservedServices) {
        return new CreateReservationCommand(TENANT_ID, "customer-1", BigDecimal.valueOf(200), reservedServices,
                "1000", List.of());
    }

    @Test
    void createsAReservationWithoutTransport() {
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(Tenant.create(TENANT_ID, "Travesia")));
        when(reservationRepositoryPort.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Reservation result = createReservationService.createReservation(aCommand(List.of(aReservedService())));

        assertThat(result.customerId()).isEqualTo("customer-1");
    }

    @Test
    void rejectsWhenTenantDoesNotExist() {
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> createReservationService.createReservation(aCommand(List.of(aReservedService()))))
                .isInstanceOf(TenantNotFoundException.class);
    }

    @Test
    void rejectsWhenTenantIsInactive() {
        Tenant inactiveTenant = Tenant.create(TENANT_ID, "Travesia").deactivate();
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(inactiveTenant));

        assertThatThrownBy(() -> createReservationService.createReservation(aCommand(List.of(aReservedService()))))
                .isInstanceOf(TenantInactiveException.class);
    }

    @Test
    void rejectsWhenTransportItemDoesNotExist() {
        UUID transportItemId = UUID.randomUUID();
        ReservedService withTransport = new ReservedService("tour-laguna", 2, LocalDate.now().plusDays(5),
                transportItemId, null);
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(Tenant.create(TENANT_ID, "Travesia")));
        when(catalogItemRepositoryPort.findByTenantIdAndCatalogItemId(TENANT_ID, transportItemId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> createReservationService.createReservation(aCommand(List.of(withTransport))))
                .isInstanceOf(InvalidReservationException.class);
    }

    @Test
    void rejectsWhenTransportItemIsNotOfTransportType() {
        UUID transportItemId = UUID.randomUUID();
        CatalogItem tourItem = CatalogItem.create(TENANT_ID, CatalogItemType.TOUR, "Laguna Verde",
                BigDecimal.valueOf(50), null, null, null, null, null, null, null, null);
        ReservedService withTransport = new ReservedService("tour-laguna", 2, LocalDate.now().plusDays(5),
                transportItemId, null);
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(Tenant.create(TENANT_ID, "Travesia")));
        when(catalogItemRepositoryPort.findByTenantIdAndCatalogItemId(TENANT_ID, transportItemId))
                .thenReturn(Optional.of(tourItem));

        assertThatThrownBy(() -> createReservationService.createReservation(aCommand(List.of(withTransport))))
                .isInstanceOf(InvalidReservationException.class);
    }
}
