package com.corhuila.errorcapa8.travesia_natural.reservations.application;

import com.corhuila.errorcapa8.travesia_natural.common.audit.AuditRecord;
import com.corhuila.errorcapa8.travesia_natural.common.audit.AuditRecorder;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.exception.ReservationNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.PaymentStatus;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.Reservation;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.ReservationStatus;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.in.ApplyDiscountCommand;
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
class ApplyDiscountReservationServiceTest {

    private static final String TENANT_ID = "travesia-natural";

    @Mock
    private TenantRepositoryPort tenantRepositoryPort;
    @Mock
    private ReservationRepositoryPort reservationRepositoryPort;
    @Mock
    private AuditRecorder auditRecorder;

    private ApplyDiscountReservationService applyDiscountReservationService;

    @BeforeEach
    void setUp() {
        applyDiscountReservationService = new ApplyDiscountReservationService(tenantRepositoryPort,
                reservationRepositoryPort, auditRecorder);
    }

    private Reservation aConfirmedReservation(UUID reservationId) {
        return Reservation.reconstitute(reservationId, TENANT_ID, "customer-1", List.of(), BigDecimal.valueOf(100),
                BigDecimal.valueOf(100), BigDecimal.ZERO, BigDecimal.ZERO, ReservationStatus.CONFIRMADA,
                PaymentStatus.PAGADO, "EFECTIVO", Instant.now(), null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                List.of());
    }

    @Test
    void appliesADiscountAndRecordsAnAuditEntry() {
        UUID reservationId = UUID.randomUUID();
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(Tenant.create(TENANT_ID, "Travesia")));
        when(reservationRepositoryPort.findByTenantIdAndReservationId(TENANT_ID, reservationId))
                .thenReturn(Optional.of(aConfirmedReservation(reservationId)));
        when(reservationRepositoryPort.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(auditRecorder.record(any(AuditRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Reservation result = applyDiscountReservationService.applyDiscount(
                new ApplyDiscountCommand(TENANT_ID, reservationId, 10, "promocion", "actor-1"));

        assertThat(result.finalValue()).isEqualByComparingTo(BigDecimal.valueOf(90));

        ArgumentCaptor<AuditRecord> auditCaptor = ArgumentCaptor.forClass(AuditRecord.class);
        verify(auditRecorder).record(auditCaptor.capture());
        assertThat(auditCaptor.getValue().action()).isEqualTo("RESERVATION_DISCOUNT_APPLIED");
    }

    @Test
    void rejectsWhenTenantDoesNotExist() {
        UUID reservationId = UUID.randomUUID();
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> applyDiscountReservationService.applyDiscount(
                new ApplyDiscountCommand(TENANT_ID, reservationId, 10, "promocion", "actor-1")))
                .isInstanceOf(TenantNotFoundException.class);
    }

    @Test
    void rejectsWhenTenantIsInactive() {
        UUID reservationId = UUID.randomUUID();
        Tenant inactiveTenant = Tenant.create(TENANT_ID, "Travesia").deactivate();
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(inactiveTenant));

        assertThatThrownBy(() -> applyDiscountReservationService.applyDiscount(
                new ApplyDiscountCommand(TENANT_ID, reservationId, 10, "promocion", "actor-1")))
                .isInstanceOf(TenantInactiveException.class);
    }

    @Test
    void rejectsWhenReservationDoesNotExist() {
        UUID reservationId = UUID.randomUUID();
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(Tenant.create(TENANT_ID, "Travesia")));
        when(reservationRepositoryPort.findByTenantIdAndReservationId(TENANT_ID, reservationId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> applyDiscountReservationService.applyDiscount(
                new ApplyDiscountCommand(TENANT_ID, reservationId, 10, "promocion", "actor-1")))
                .isInstanceOf(ReservationNotFoundException.class);
    }
}
