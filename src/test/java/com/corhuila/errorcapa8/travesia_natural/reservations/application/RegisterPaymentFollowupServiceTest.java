package com.corhuila.errorcapa8.travesia_natural.reservations.application;

import com.corhuila.errorcapa8.travesia_natural.common.audit.AuditRecord;
import com.corhuila.errorcapa8.travesia_natural.common.audit.AuditRecorder;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.exception.InvalidReservationException;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.exception.ReservationNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.PaymentStatus;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.Reservation;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.ReservationStatus;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.in.RegisterPaymentFollowupCommand;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.out.ReservationRepositoryPort;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantInactiveException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.Tenant;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.out.TenantRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterPaymentFollowupServiceTest {

    private static final String TENANT_ID = "travesia-natural";

    @Mock
    private TenantRepositoryPort tenantRepositoryPort;
    @Mock
    private ReservationRepositoryPort reservationRepositoryPort;
    @Mock
    private AuditRecorder auditRecorder;

    private RegisterPaymentFollowupService registerPaymentFollowupService;

    @BeforeEach
    void setUp() {
        registerPaymentFollowupService = new RegisterPaymentFollowupService(tenantRepositoryPort,
                reservationRepositoryPort, auditRecorder);
    }

    private Reservation aPendingReservation(UUID reservationId) {
        return Reservation.reconstitute(reservationId, TENANT_ID, "customer-1", List.of(), BigDecimal.valueOf(100),
                BigDecimal.valueOf(100), BigDecimal.valueOf(100), BigDecimal.ZERO, ReservationStatus.PENDIENTE_DE_PAGO,
                PaymentStatus.SIN_PAGO, null, Instant.now(), null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, List.of());
    }

    @Test
    void registersAFollowup() {
        UUID reservationId = UUID.randomUUID();
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(Tenant.create(TENANT_ID, "Travesia")));
        when(reservationRepositoryPort.findByTenantIdAndReservationId(TENANT_ID, reservationId))
                .thenReturn(Optional.of(aPendingReservation(reservationId)));
        when(auditRecorder.record(any(AuditRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        registerPaymentFollowupService.registerFollowup(new RegisterPaymentFollowupCommand(TENANT_ID, reservationId,
                "cliente confirma pago manana", "actor-1"));

        ArgumentCaptor<AuditRecord> captor = ArgumentCaptor.forClass(AuditRecord.class);
        verify(auditRecorder).record(captor.capture());
        assertThat(captor.getValue().action()).isEqualTo("SEGUIMIENTO_PAGO");
        assertThat(captor.getValue().affectedRecordId()).isEqualTo(reservationId.toString());
    }

    @Test
    void rejectsWhenNoteIsBlank() {
        UUID reservationId = UUID.randomUUID();

        assertThatThrownBy(() -> registerPaymentFollowupService.registerFollowup(
                new RegisterPaymentFollowupCommand(TENANT_ID, reservationId, " ", "actor-1")))
                .isInstanceOf(InvalidReservationException.class);
    }

    @Test
    void rejectsWhenTenantDoesNotExist() {
        UUID reservationId = UUID.randomUUID();
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> registerPaymentFollowupService.registerFollowup(
                new RegisterPaymentFollowupCommand(TENANT_ID, reservationId, "seguimiento", "actor-1")))
                .isInstanceOf(TenantNotFoundException.class);
    }

    @Test
    void rejectsWhenTenantIsInactive() {
        UUID reservationId = UUID.randomUUID();
        Tenant inactiveTenant = Tenant.create(TENANT_ID, "Travesia").deactivate();
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(inactiveTenant));

        assertThatThrownBy(() -> registerPaymentFollowupService.registerFollowup(
                new RegisterPaymentFollowupCommand(TENANT_ID, reservationId, "seguimiento", "actor-1")))
                .isInstanceOf(TenantInactiveException.class);
    }

    @Test
    void rejectsWhenReservationDoesNotExist() {
        UUID reservationId = UUID.randomUUID();
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(Tenant.create(TENANT_ID, "Travesia")));
        when(reservationRepositoryPort.findByTenantIdAndReservationId(TENANT_ID, reservationId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> registerPaymentFollowupService.registerFollowup(
                new RegisterPaymentFollowupCommand(TENANT_ID, reservationId, "seguimiento", "actor-1")))
                .isInstanceOf(ReservationNotFoundException.class);
    }
}
