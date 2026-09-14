package com.corhuila.errorcapa8.travesia_natural.reservations.application;

import com.corhuila.errorcapa8.travesia_natural.reservations.domain.exception.ReservationNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.PaymentStatus;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.RefundDecisionStatus;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.Reservation;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.ReservationStatus;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.in.RefundReservationCommand;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefundReservationServiceTest {

    private static final String TENANT_ID = "travesia-natural";

    @Mock
    private TenantRepositoryPort tenantRepositoryPort;
    @Mock
    private ReservationRepositoryPort reservationRepositoryPort;

    private RefundReservationService refundReservationService;

    @BeforeEach
    void setUp() {
        refundReservationService = new RefundReservationService(tenantRepositoryPort, reservationRepositoryPort);
    }

    private Reservation anAuthorizedReservation(UUID reservationId) {
        return Reservation.reconstitute(reservationId, TENANT_ID, "customer-1", List.of(), BigDecimal.valueOf(100),
                BigDecimal.valueOf(100), BigDecimal.ZERO, BigDecimal.valueOf(100),
                ReservationStatus.CANCELADA, PaymentStatus.SALDO_A_FAVOR_PENDIENTE, null, Instant.now(), null, null,
                "cancelada", "actor-1", Instant.now(), RefundDecisionStatus.AUTORIZADA, "admin-1", Instant.now(),
                "aprobado", null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                List.of());
    }

    @Test
    void refundsAReservation() {
        UUID reservationId = UUID.randomUUID();
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(Tenant.create(TENANT_ID, "Travesia")));
        when(reservationRepositoryPort.findByTenantIdAndReservationId(TENANT_ID, reservationId))
                .thenReturn(Optional.of(anAuthorizedReservation(reservationId)));
        when(reservationRepositoryPort.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Reservation result = refundReservationService.refundReservation(new RefundReservationCommand(TENANT_ID,
                reservationId, BigDecimal.valueOf(100), "devolucion total", "actor-1", "TRANSFERENCIA"));

        assertThat(result.refundDecisionStatus()).isEqualTo(RefundDecisionStatus.EJECUTADA);
        assertThat(result.refundedAmount()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    @Test
    void rejectsWhenTenantDoesNotExist() {
        UUID reservationId = UUID.randomUUID();
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refundReservationService.refundReservation(new RefundReservationCommand(TENANT_ID,
                reservationId, BigDecimal.valueOf(100), "devolucion total", "actor-1", "TRANSFERENCIA")))
                .isInstanceOf(TenantNotFoundException.class);
    }

    @Test
    void rejectsWhenTenantIsInactive() {
        UUID reservationId = UUID.randomUUID();
        Tenant inactiveTenant = Tenant.create(TENANT_ID, "Travesia").deactivate();
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(inactiveTenant));

        assertThatThrownBy(() -> refundReservationService.refundReservation(new RefundReservationCommand(TENANT_ID,
                reservationId, BigDecimal.valueOf(100), "devolucion total", "actor-1", "TRANSFERENCIA")))
                .isInstanceOf(TenantInactiveException.class);
    }

    @Test
    void rejectsWhenReservationDoesNotExist() {
        UUID reservationId = UUID.randomUUID();
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(Tenant.create(TENANT_ID, "Travesia")));
        when(reservationRepositoryPort.findByTenantIdAndReservationId(TENANT_ID, reservationId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> refundReservationService.refundReservation(new RefundReservationCommand(TENANT_ID,
                reservationId, BigDecimal.valueOf(100), "devolucion total", "actor-1", "TRANSFERENCIA")))
                .isInstanceOf(ReservationNotFoundException.class);
    }
}
