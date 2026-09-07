package com.corhuila.errorcapa8.travesia_natural.reservations.application;

import com.corhuila.errorcapa8.travesia_natural.reservations.domain.exception.ReservationNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.PaymentStatus;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.Reservation;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.ReservationStatus;
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
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationQueryServiceTest {

    private static final String TENANT_ID = "travesia-natural";

    @Mock
    private TenantRepositoryPort tenantRepositoryPort;
    @Mock
    private ReservationRepositoryPort reservationRepositoryPort;

    private ReservationQueryService reservationQueryService;

    @BeforeEach
    void setUp() {
        reservationQueryService = new ReservationQueryService(tenantRepositoryPort, reservationRepositoryPort);
    }

    private Reservation aReservation(UUID reservationId, String customerId, ReservationStatus status,
                                      PaymentStatus paymentStatus) {
        return Reservation.reconstitute(reservationId, TENANT_ID, customerId, List.of(), BigDecimal.TEN,
                BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO, status, paymentStatus, "EFECTIVO", Instant.now(),
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, List.of());
    }

    @Test
    void getsReservationById() {
        UUID reservationId = UUID.randomUUID();
        Reservation reservation = aReservation(reservationId, "customer-1", ReservationStatus.CONFIRMADA,
                PaymentStatus.PAGADO);
        when(tenantRepositoryPort.existsById(TENANT_ID)).thenReturn(true);
        when(reservationRepositoryPort.findByTenantIdAndReservationId(TENANT_ID, reservationId))
                .thenReturn(Optional.of(reservation));

        assertThat(reservationQueryService.getById(TENANT_ID, reservationId)).isEqualTo(reservation);
    }

    @Test
    void rejectsGetByIdWhenTenantDoesNotExist() {
        UUID reservationId = UUID.randomUUID();
        when(tenantRepositoryPort.existsById(TENANT_ID)).thenReturn(false);

        assertThatThrownBy(() -> reservationQueryService.getById(TENANT_ID, reservationId))
                .isInstanceOf(TenantNotFoundException.class);
    }

    @Test
    void rejectsGetByIdWhenReservationDoesNotExist() {
        UUID reservationId = UUID.randomUUID();
        when(tenantRepositoryPort.existsById(TENANT_ID)).thenReturn(true);
        when(reservationRepositoryPort.findByTenantIdAndReservationId(TENANT_ID, reservationId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservationQueryService.getById(TENANT_ID, reservationId))
                .isInstanceOf(ReservationNotFoundException.class);
    }

    @Test
    void listsReservationsOfATenant() {
        Reservation reservation = aReservation(UUID.randomUUID(), "customer-1", ReservationStatus.CONFIRMADA,
                PaymentStatus.PAGADO);
        when(tenantRepositoryPort.existsById(TENANT_ID)).thenReturn(true);
        when(reservationRepositoryPort.findAllByTenantId(TENANT_ID)).thenReturn(List.of(reservation));

        assertThat(reservationQueryService.listByTenant(TENANT_ID)).containsExactly(reservation);
    }

    @Test
    void listsReservationsOfATenantAndCustomer() {
        Reservation reservation = aReservation(UUID.randomUUID(), "customer-1", ReservationStatus.CONFIRMADA,
                PaymentStatus.PAGADO);
        when(tenantRepositoryPort.existsById(TENANT_ID)).thenReturn(true);
        when(reservationRepositoryPort.findAllByTenantIdAndCustomerId(TENANT_ID, "customer-1"))
                .thenReturn(List.of(reservation));

        assertThat(reservationQueryService.listByTenantAndCustomer(TENANT_ID, "customer-1"))
                .containsExactly(reservation);
    }

    @Test
    void getsReservationByIdForItsOwningCustomer() {
        UUID reservationId = UUID.randomUUID();
        Reservation reservation = aReservation(reservationId, "customer-1", ReservationStatus.CONFIRMADA,
                PaymentStatus.PAGADO);
        when(tenantRepositoryPort.existsById(TENANT_ID)).thenReturn(true);
        when(reservationRepositoryPort.findByTenantIdAndReservationId(TENANT_ID, reservationId))
                .thenReturn(Optional.of(reservation));

        assertThat(reservationQueryService.getByIdForCustomer(TENANT_ID, "customer-1", reservationId))
                .isEqualTo(reservation);
    }

    @Test
    void rejectsGetByIdForCustomerWhenReservationBelongsToSomeoneElse() {
        UUID reservationId = UUID.randomUUID();
        Reservation reservation = aReservation(reservationId, "customer-1", ReservationStatus.CONFIRMADA,
                PaymentStatus.PAGADO);
        when(tenantRepositoryPort.existsById(TENANT_ID)).thenReturn(true);
        when(reservationRepositoryPort.findByTenantIdAndReservationId(TENANT_ID, reservationId))
                .thenReturn(Optional.of(reservation));

        assertThatThrownBy(
                () -> reservationQueryService.getByIdForCustomer(TENANT_ID, "someone-else", reservationId))
                .isInstanceOf(ReservationNotFoundException.class);
    }

    @Test
    void listsPendingSupportReservations() {
        Reservation pending = aReservation(UUID.randomUUID(), "customer-1", ReservationStatus.PENDIENTE_DE_PAGO,
                PaymentStatus.EN_VALIDACION);
        Reservation notPending = aReservation(UUID.randomUUID(), "customer-2", ReservationStatus.CONFIRMADA,
                PaymentStatus.PAGADO);
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(Tenant.create(TENANT_ID, "Travesia")));
        when(reservationRepositoryPort.findAllByTenantId(TENANT_ID)).thenReturn(List.of(pending, notPending));

        assertThat(reservationQueryService.listPendingSupportByTenant(TENANT_ID)).containsExactly(pending);
    }

    @Test
    void rejectsListPendingSupportWhenTenantIsInactive() {
        Tenant inactiveTenant = Tenant.create(TENANT_ID, "Travesia").deactivate();
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(inactiveTenant));

        assertThatThrownBy(() -> reservationQueryService.listPendingSupportByTenant(TENANT_ID))
                .isInstanceOf(TenantInactiveException.class);
    }

    @Test
    void listsPendingExecutionReservations() {
        Reservation confirmed = aReservation(UUID.randomUUID(), "customer-1", ReservationStatus.CONFIRMADA,
                PaymentStatus.PAGADO);
        Reservation executing = aReservation(UUID.randomUUID(), "customer-2", ReservationStatus.EN_EJECUCION,
                PaymentStatus.PAGADO);
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(Tenant.create(TENANT_ID, "Travesia")));
        when(reservationRepositoryPort.findAllByTenantId(TENANT_ID)).thenReturn(List.of(confirmed, executing));

        assertThat(reservationQueryService.listPendingExecutionByTenant(TENANT_ID)).containsExactly(confirmed);
    }
}
