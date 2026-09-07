package com.corhuila.errorcapa8.travesia_natural.reservations.application;

import com.corhuila.errorcapa8.travesia_natural.reservations.domain.exception.InvalidReservationException;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.exception.ReservationNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.PaymentStatus;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.Reservation;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.ReservationStatus;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.in.RegisterPaymentCommand;
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
class RegisterPaymentServiceTest {

    private static final String TENANT_ID = "travesia-natural";

    @Mock
    private TenantRepositoryPort tenantRepositoryPort;
    @Mock
    private ReservationRepositoryPort reservationRepositoryPort;

    private RegisterPaymentService registerPaymentService;

    @BeforeEach
    void setUp() {
        registerPaymentService = new RegisterPaymentService(tenantRepositoryPort, reservationRepositoryPort);
    }

    private Reservation aPendingReservation(UUID reservationId) {
        return Reservation.reconstitute(reservationId, TENANT_ID, "customer-1", List.of(), BigDecimal.valueOf(100),
                BigDecimal.valueOf(100), BigDecimal.valueOf(100), BigDecimal.ZERO, ReservationStatus.PENDIENTE_DE_PAGO,
                PaymentStatus.SIN_PAGO, null, Instant.now(), null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, List.of());
    }

    @Test
    void registersACashPayment() {
        UUID reservationId = UUID.randomUUID();
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(Tenant.create(TENANT_ID, "Travesia")));
        when(reservationRepositoryPort.findByTenantIdAndReservationId(TENANT_ID, reservationId))
                .thenReturn(Optional.of(aPendingReservation(reservationId)));
        when(reservationRepositoryPort.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Reservation result = registerPaymentService.registerPayment(
                new RegisterPaymentCommand(TENANT_ID, reservationId, "EFECTIVO", BigDecimal.valueOf(100), null));

        assertThat(result.paymentStatus()).isEqualTo(PaymentStatus.PAGADO);
    }

    @Test
    void registersAnInstallmentPayment() {
        UUID reservationId = UUID.randomUUID();
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(Tenant.create(TENANT_ID, "Travesia")));
        when(reservationRepositoryPort.findByTenantIdAndReservationId(TENANT_ID, reservationId))
                .thenReturn(Optional.of(aPendingReservation(reservationId)));
        when(reservationRepositoryPort.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Reservation result = registerPaymentService.registerPayment(
                new RegisterPaymentCommand(TENANT_ID, reservationId, "ABONO", BigDecimal.valueOf(40), null));

        assertThat(result.pendingBalance()).isEqualByComparingTo(BigDecimal.valueOf(60));
    }

    @Test
    void registersATransferPayment() {
        UUID reservationId = UUID.randomUUID();
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(Tenant.create(TENANT_ID, "Travesia")));
        when(reservationRepositoryPort.findByTenantIdAndReservationId(TENANT_ID, reservationId))
                .thenReturn(Optional.of(aPendingReservation(reservationId)));
        when(reservationRepositoryPort.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Reservation result = registerPaymentService.registerPayment(
                new RegisterPaymentCommand(TENANT_ID, reservationId, "TRANSFERENCIA", BigDecimal.valueOf(100),
                        "soporte-1"));

        assertThat(result.paymentStatus()).isEqualTo(PaymentStatus.EN_VALIDACION);
    }

    @Test
    void rejectsAnUnknownPaymentMethod() {
        UUID reservationId = UUID.randomUUID();
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(Tenant.create(TENANT_ID, "Travesia")));
        when(reservationRepositoryPort.findByTenantIdAndReservationId(TENANT_ID, reservationId))
                .thenReturn(Optional.of(aPendingReservation(reservationId)));

        assertThatThrownBy(() -> registerPaymentService.registerPayment(
                new RegisterPaymentCommand(TENANT_ID, reservationId, "BITCOIN", BigDecimal.valueOf(100), null)))
                .isInstanceOf(InvalidReservationException.class);
    }

    @Test
    void rejectsWhenTenantDoesNotExist() {
        UUID reservationId = UUID.randomUUID();
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> registerPaymentService.registerPayment(
                new RegisterPaymentCommand(TENANT_ID, reservationId, "EFECTIVO", BigDecimal.valueOf(100), null)))
                .isInstanceOf(TenantNotFoundException.class);
    }

    @Test
    void rejectsWhenTenantIsInactive() {
        UUID reservationId = UUID.randomUUID();
        Tenant inactiveTenant = Tenant.create(TENANT_ID, "Travesia").deactivate();
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(inactiveTenant));

        assertThatThrownBy(() -> registerPaymentService.registerPayment(
                new RegisterPaymentCommand(TENANT_ID, reservationId, "EFECTIVO", BigDecimal.valueOf(100), null)))
                .isInstanceOf(TenantInactiveException.class);
    }

    @Test
    void rejectsWhenReservationDoesNotExist() {
        UUID reservationId = UUID.randomUUID();
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(Tenant.create(TENANT_ID, "Travesia")));
        when(reservationRepositoryPort.findByTenantIdAndReservationId(TENANT_ID, reservationId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> registerPaymentService.registerPayment(
                new RegisterPaymentCommand(TENANT_ID, reservationId, "EFECTIVO", BigDecimal.valueOf(100), null)))
                .isInstanceOf(ReservationNotFoundException.class);
    }
}
