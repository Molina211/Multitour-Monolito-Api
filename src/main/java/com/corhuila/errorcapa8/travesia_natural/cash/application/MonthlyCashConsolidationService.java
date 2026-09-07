package com.corhuila.errorcapa8.travesia_natural.cash.application;

import com.corhuila.errorcapa8.travesia_natural.cash.domain.model.CashMovement;
import com.corhuila.errorcapa8.travesia_natural.cash.domain.model.CashMovementType;
import com.corhuila.errorcapa8.travesia_natural.cash.domain.model.CashRegister;
import com.corhuila.errorcapa8.travesia_natural.cash.domain.model.MonthlyCashConsolidation;
import com.corhuila.errorcapa8.travesia_natural.cash.domain.port.in.MonthlyCashConsolidationQueryUseCase;
import com.corhuila.errorcapa8.travesia_natural.cash.domain.port.out.CashRegisterRepositoryPort;
import com.corhuila.errorcapa8.travesia_natural.operations.domain.model.OperationCost;
import com.corhuila.errorcapa8.travesia_natural.operations.domain.port.out.OperationCostRepositoryPort;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.Reservation;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.ReservationStatus;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.out.ReservationRepositoryPort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class MonthlyCashConsolidationService implements MonthlyCashConsolidationQueryUseCase {

    private final TenantGuard tenantGuard;
    private final CashRegisterRepositoryPort cashRegisterRepositoryPort;
    private final ReservationRepositoryPort reservationRepositoryPort;
    private final OperationCostRepositoryPort operationCostRepositoryPort;
    private final RefundsTotalCalculator refundsTotalCalculator;

    public MonthlyCashConsolidationService(TenantGuard tenantGuard,
                                            CashRegisterRepositoryPort cashRegisterRepositoryPort,
                                            ReservationRepositoryPort reservationRepositoryPort,
                                            OperationCostRepositoryPort operationCostRepositoryPort,
                                            RefundsTotalCalculator refundsTotalCalculator) {
        this.tenantGuard = tenantGuard;
        this.cashRegisterRepositoryPort = cashRegisterRepositoryPort;
        this.reservationRepositoryPort = reservationRepositoryPort;
        this.operationCostRepositoryPort = operationCostRepositoryPort;
        this.refundsTotalCalculator = refundsTotalCalculator;
    }

    @Override
    public List<MonthlyCashConsolidation> getMonthlyConsolidation(String tenantId, YearMonth period) {
        tenantGuard.requireActive(tenantId);

        List<CashRegister> closedInPeriod = cashRegisterRepositoryPort.findAllClosedByTenantId(tenantId).stream()
                .filter(cashRegister -> YearMonth.from(cashRegister.businessDate()).equals(period))
                .toList();

        BigDecimal ingresos = sumMovements(closedInPeriod, CashMovementType.INGRESO);
        BigDecimal pagosOperacionales = sumMovements(closedInPeriod, CashMovementType.PAGO);
        BigDecimal gastos = sumMovements(closedInPeriod, CashMovementType.GASTO);

        List<Reservation> reservations = reservationRepositoryPort.findAllByTenantId(tenantId);

        BigDecimal devoluciones = refundsTotalCalculator.totalForPeriod(reservations, period);
        BigDecimal total = ingresos.subtract(pagosOperacionales).subtract(gastos).subtract(devoluciones);

        long cancelaciones = reservations.stream()
                .filter(reservation -> reservation.reservationStatus() == ReservationStatus.CANCELADA)
                .filter(reservation -> reservation.cancelledAt() != null)
                .filter(reservation -> YearMonth.from(reservation.cancelledAt().atZone(ZoneOffset.UTC).toLocalDate())
                        .equals(period))
                .count();

        BigDecimal costosOperacionales = operationCostRepositoryPort.findAllByTenantId(tenantId).stream()
                .filter(cost -> YearMonth.from(cost.recordedAt().atZone(ZoneOffset.UTC).toLocalDate())
                        .equals(period))
                .map(OperationCost::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return List.of(new MonthlyCashConsolidation(period.toString(), ingresos, pagosOperacionales, gastos,
                devoluciones, total, cancelaciones, costosOperacionales));
    }

    private static BigDecimal sumMovements(List<CashRegister> closedInPeriod, CashMovementType type) {
        return closedInPeriod.stream()
                .flatMap(cashRegister -> cashRegister.movements().stream())
                .filter(movement -> movement.type() == type)
                .map(CashMovement::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
