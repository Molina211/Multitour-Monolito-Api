package com.corhuila.errorcapa8.travesia_natural.reservations.application;

import com.corhuila.errorcapa8.travesia_natural.common.audit.AuditRecord;
import com.corhuila.errorcapa8.travesia_natural.common.audit.AuditRecorder;
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
class PaymentFollowupQueryServiceTest {

    private static final String TENANT_ID = "travesia-natural";

    @Mock
    private TenantRepositoryPort tenantRepositoryPort;
    @Mock
    private ReservationRepositoryPort reservationRepositoryPort;
    @Mock
    private AuditRecorder auditRecorder;

    private PaymentFollowupQueryService paymentFollowupQueryService;

    @BeforeEach
    void setUp() {
        paymentFollowupQueryService = new PaymentFollowupQueryService(tenantRepositoryPort,
                reservationRepositoryPort, auditRecorder);
    }

    private Reservation aPendingReservation(UUID reservationId) {
        return Reservation.reconstitute(reservationId, TENANT_ID, "customer-1", List.of(), BigDecimal.valueOf(100),
                BigDecimal.valueOf(100), BigDecimal.valueOf(100), BigDecimal.ZERO, ReservationStatus.PENDIENTE_DE_PAGO,
                PaymentStatus.SIN_PAGO, null, Instant.now(), null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, List.of());
    }

    @Test
    void listsFollowupsForAReservation() {
        UUID reservationId = UUID.randomUUID();
        AuditRecord matching = AuditRecord.of(TENANT_ID, "actor-1", "SEGUIMIENTO_PAGO", reservationId.toString(),
                "cliente confirma pago manana");
        AuditRecord otherAction = AuditRecord.of(TENANT_ID, "actor-1", "OTRA_ACCION", reservationId.toString(),
                "no aplica");
        AuditRecord otherReservation = AuditRecord.of(TENANT_ID, "actor-1", "SEGUIMIENTO_PAGO",
                UUID.randomUUID().toString(), "no aplica");
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(Tenant.create(TENANT_ID, "Travesia")));
        when(reservationRepositoryPort.findByTenantIdAndReservationId(TENANT_ID, reservationId))
                .thenReturn(Optional.of(aPendingReservation(reservationId)));
        when(auditRecorder.findAll()).thenReturn(List.of(matching, otherAction, otherReservation));

        List<AuditRecord> result = paymentFollowupQueryService.listFollowups(TENANT_ID, reservationId);

        assertThat(result).containsExactly(matching);
    }

    @Test
    void rejectsWhenTenantDoesNotExist() {
        UUID reservationId = UUID.randomUUID();
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentFollowupQueryService.listFollowups(TENANT_ID, reservationId))
                .isInstanceOf(TenantNotFoundException.class);
    }

    @Test
    void rejectsWhenTenantIsInactive() {
        UUID reservationId = UUID.randomUUID();
        Tenant inactiveTenant = Tenant.create(TENANT_ID, "Travesia").deactivate();
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(inactiveTenant));

        assertThatThrownBy(() -> paymentFollowupQueryService.listFollowups(TENANT_ID, reservationId))
                .isInstanceOf(TenantInactiveException.class);
    }

    @Test
    void rejectsWhenReservationDoesNotExist() {
        UUID reservationId = UUID.randomUUID();
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(Tenant.create(TENANT_ID, "Travesia")));
        when(reservationRepositoryPort.findByTenantIdAndReservationId(TENANT_ID, reservationId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentFollowupQueryService.listFollowups(TENANT_ID, reservationId))
                .isInstanceOf(ReservationNotFoundException.class);
    }
}
