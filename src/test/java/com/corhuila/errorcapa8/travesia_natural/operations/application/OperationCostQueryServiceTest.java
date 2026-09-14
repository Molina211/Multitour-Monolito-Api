package com.corhuila.errorcapa8.travesia_natural.operations.application;

import com.corhuila.errorcapa8.travesia_natural.operations.domain.model.OperationCost;
import com.corhuila.errorcapa8.travesia_natural.operations.domain.port.out.OperationCostRepositoryPort;
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
class OperationCostQueryServiceTest {

    private static final String TENANT_ID = "travesia-natural";

    @Mock
    private TenantRepositoryPort tenantRepositoryPort;
    @Mock
    private ReservationRepositoryPort reservationRepositoryPort;
    @Mock
    private OperationCostRepositoryPort operationCostRepositoryPort;

    private OperationCostQueryService operationCostQueryService;

    @BeforeEach
    void setUp() {
        operationCostQueryService = new OperationCostQueryService(tenantRepositoryPort, reservationRepositoryPort,
                operationCostRepositoryPort);
    }

    private Reservation aReservation(UUID reservationId) {
        return Reservation.reconstitute(reservationId, TENANT_ID, "customer-1", List.of(), BigDecimal.TEN,
                BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO, ReservationStatus.EN_EJECUCION,
                PaymentStatus.PAGADO, "EFECTIVO", Instant.now(), null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                List.of());
    }

    @Test
    void listsOperationCostsOfAReservation() {
        UUID reservationId = UUID.randomUUID();
        OperationCost cost = OperationCost.create(TENANT_ID, reservationId, "Transporte", BigDecimal.TEN, "actor-1");
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(Tenant.create(TENANT_ID, "Travesia")));
        when(reservationRepositoryPort.findByTenantIdAndReservationId(TENANT_ID, reservationId))
                .thenReturn(Optional.of(aReservation(reservationId)));
        when(operationCostRepositoryPort.findAllByTenantIdAndReservationIdOrderByRecordedAt(TENANT_ID, reservationId))
                .thenReturn(List.of(cost));

        assertThat(operationCostQueryService.listByReservation(TENANT_ID, reservationId)).containsExactly(cost);
    }

    @Test
    void rejectsWhenTenantDoesNotExist() {
        UUID reservationId = UUID.randomUUID();
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> operationCostQueryService.listByReservation(TENANT_ID, reservationId))
                .isInstanceOf(TenantNotFoundException.class);
    }

    @Test
    void rejectsWhenTenantIsInactive() {
        UUID reservationId = UUID.randomUUID();
        Tenant inactiveTenant = Tenant.create(TENANT_ID, "Travesia").deactivate();
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(inactiveTenant));

        assertThatThrownBy(() -> operationCostQueryService.listByReservation(TENANT_ID, reservationId))
                .isInstanceOf(TenantInactiveException.class);
    }

    @Test
    void rejectsWhenReservationDoesNotExist() {
        UUID reservationId = UUID.randomUUID();
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(Tenant.create(TENANT_ID, "Travesia")));
        when(reservationRepositoryPort.findByTenantIdAndReservationId(TENANT_ID, reservationId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> operationCostQueryService.listByReservation(TENANT_ID, reservationId))
                .isInstanceOf(ReservationNotFoundException.class);
    }
}
