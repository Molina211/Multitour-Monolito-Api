package com.corhuila.errorcapa8.travesia_natural.cash.application;

import com.corhuila.errorcapa8.travesia_natural.cash.domain.model.CashMovementType;
import com.corhuila.errorcapa8.travesia_natural.cash.domain.model.CashRegister;
import com.corhuila.errorcapa8.travesia_natural.cash.domain.model.MonthlyCashConsolidation;
import com.corhuila.errorcapa8.travesia_natural.cash.domain.port.out.CashRegisterRepositoryPort;
import com.corhuila.errorcapa8.travesia_natural.operations.domain.model.OperationCost;
import com.corhuila.errorcapa8.travesia_natural.operations.domain.port.out.OperationCostRepositoryPort;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.PaymentStatus;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.Reservation;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.ReservationStatus;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.out.ReservationRepositoryPort;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.Tenant;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.out.TenantRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonthlyCashConsolidationServiceTest {

    private static final String TENANT_ID = "travesia-natural";
    private static final YearMonth PERIOD = YearMonth.of(2026, 9);

    @Mock
    private TenantRepositoryPort tenantRepositoryPort;
    @Mock
    private CashRegisterRepositoryPort cashRegisterRepositoryPort;
    @Mock
    private ReservationRepositoryPort reservationRepositoryPort;
    @Mock
    private OperationCostRepositoryPort operationCostRepositoryPort;

    private MonthlyCashConsolidationService service;

    @BeforeEach
    void setUp() {
        TenantGuard tenantGuard = new TenantGuard(tenantRepositoryPort);
        RefundsTotalCalculator refundsTotalCalculator = new RefundsTotalCalculator(reservationRepositoryPort);
        service = new MonthlyCashConsolidationService(tenantGuard, cashRegisterRepositoryPort,
                reservationRepositoryPort, operationCostRepositoryPort, refundsTotalCalculator);
    }

    @Test
    void aggregatesIncomeExpensesRefundsAndCancellationsForThePeriod() {
        CashRegister closed = CashRegister.open(UUID.randomUUID(), TENANT_ID, LocalDate.of(2026, 9, 5),
                        BigDecimal.valueOf(50000))
                .registerMovement(CashMovementType.INGRESO, BigDecimal.valueOf(100000), "ventas", "actor-1")
                .registerMovement(CashMovementType.PAGO, BigDecimal.valueOf(20000), "proveedor", "actor-1")
                .registerMovement(CashMovementType.GASTO, BigDecimal.valueOf(5000), "papeleria", "actor-1")
                .close("actor-1", BigDecimal.ZERO);

        Instant cancelledAt = Instant.parse("2026-09-10T12:00:00Z");
        Reservation cancelled = Reservation.reconstitute(UUID.randomUUID(), TENANT_ID, "customer-1", List.of(),
                BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO, ReservationStatus.CANCELADA,
                PaymentStatus.SIN_PAGO, "EFECTIVO", Instant.now(), null, null, "cancelada", "actor-1", cancelledAt,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                "cc-1", List.of());

        OperationCost cost = OperationCost.create(TENANT_ID, UUID.randomUUID(), "combustible",
                BigDecimal.valueOf(3000), "actor-1");

        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(Tenant.create(TENANT_ID, "Travesia")));
        when(cashRegisterRepositoryPort.findAllClosedByTenantId(TENANT_ID)).thenReturn(List.of(closed));
        when(reservationRepositoryPort.findAllByTenantId(TENANT_ID)).thenReturn(List.of(cancelled));
        when(operationCostRepositoryPort.findAllByTenantId(TENANT_ID)).thenReturn(List.of(cost));

        List<MonthlyCashConsolidation> result = service.getMonthlyConsolidation(TENANT_ID, PERIOD);

        assertThat(result).hasSize(1);
        MonthlyCashConsolidation consolidation = result.get(0);
        assertThat(consolidation.ingresos()).isEqualByComparingTo(BigDecimal.valueOf(100000));
        assertThat(consolidation.pagosOperacionales()).isEqualByComparingTo(BigDecimal.valueOf(20000));
        assertThat(consolidation.gastos()).isEqualByComparingTo(BigDecimal.valueOf(5000));
        assertThat(consolidation.devoluciones()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(consolidation.total()).isEqualByComparingTo(BigDecimal.valueOf(75000));
        assertThat(consolidation.cancelaciones()).isEqualTo(1L);
        assertThat(consolidation.costosOperacionales()).isEqualByComparingTo(BigDecimal.valueOf(3000));
    }

    @Test
    void excludesClosedCashRegistersOutsideThePeriod() {
        CashRegister closedOutsidePeriod = CashRegister.open(UUID.randomUUID(), TENANT_ID,
                        LocalDate.of(2026, 8, 5), BigDecimal.valueOf(50000))
                .registerMovement(CashMovementType.INGRESO, BigDecimal.valueOf(100000), "ventas", "actor-1")
                .close("actor-1", BigDecimal.ZERO);

        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(Tenant.create(TENANT_ID, "Travesia")));
        when(cashRegisterRepositoryPort.findAllClosedByTenantId(TENANT_ID)).thenReturn(List.of(closedOutsidePeriod));
        when(reservationRepositoryPort.findAllByTenantId(TENANT_ID)).thenReturn(List.of());
        when(operationCostRepositoryPort.findAllByTenantId(TENANT_ID)).thenReturn(List.of());

        MonthlyCashConsolidation consolidation = service.getMonthlyConsolidation(TENANT_ID, PERIOD).get(0);

        assertThat(consolidation.ingresos()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
