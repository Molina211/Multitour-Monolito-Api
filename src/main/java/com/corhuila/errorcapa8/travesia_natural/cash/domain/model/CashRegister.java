package com.corhuila.errorcapa8.travesia_natural.cash.domain.model;

import com.corhuila.errorcapa8.travesia_natural.cash.domain.exception.CashRegisterClosedException;
import com.corhuila.errorcapa8.travesia_natural.cash.domain.exception.CashRegisterNotClosedException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Aggregate root para la caja diaria (spec 013): una caja por {@code tenantId} +
 * {@code businessDate}, con sus movimientos y correcciones embebidos. Las devoluciones
 * (spec 012) no son un {@link CashMovement} persistido: se calculan en vivo y se pasan
 * como {@code refundsTotal} a {@link #computeTotal(BigDecimal)}/{@link #close}.
 */
public final class CashRegister {

    private final UUID cashRegisterId;
    private final String tenantId;
    private final LocalDate businessDate;
    private final BigDecimal baseAmount;
    private final CashRegisterStatus status;
    private final List<CashMovement> movements;
    private final List<CashCorrection> corrections;
    private final String closedBy;
    private final Instant closedAt;
    private final BigDecimal totalAmount;

    private CashRegister(UUID cashRegisterId, String tenantId, LocalDate businessDate, BigDecimal baseAmount,
                          CashRegisterStatus status, List<CashMovement> movements, List<CashCorrection> corrections,
                          String closedBy, Instant closedAt, BigDecimal totalAmount) {
        this.cashRegisterId = cashRegisterId;
        this.tenantId = tenantId;
        this.businessDate = businessDate;
        this.baseAmount = baseAmount;
        this.status = status;
        this.movements = movements;
        this.corrections = corrections;
        this.closedBy = closedBy;
        this.closedAt = closedAt;
        this.totalAmount = totalAmount;
    }

    public static CashRegister open(UUID cashRegisterId, String tenantId, LocalDate businessDate,
                                     BigDecimal baseAmount) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId is required");
        }
        if (businessDate == null) {
            throw new IllegalArgumentException("businessDate is required");
        }
        if (baseAmount == null || baseAmount.signum() < 0) {
            throw new IllegalArgumentException("baseAmount must be a non-negative amount");
        }
        return new CashRegister(cashRegisterId, tenantId, businessDate, baseAmount, CashRegisterStatus.ABIERTA,
                List.of(), List.of(), null, null, null);
    }

    /**
     * Reconstruye una caja exactamente como quedó persistida, sin re-validar invariantes
     * de apertura (mismo patrón que {@code Reservation.reconstitute(...)}).
     */
    public static CashRegister reconstitute(UUID cashRegisterId, String tenantId, LocalDate businessDate,
                                             BigDecimal baseAmount, CashRegisterStatus status,
                                             List<CashMovement> movements, List<CashCorrection> corrections,
                                             String closedBy, Instant closedAt, BigDecimal totalAmount) {
        return new CashRegister(cashRegisterId, tenantId, businessDate, baseAmount, status, List.copyOf(movements),
                List.copyOf(corrections), closedBy, closedAt, totalAmount);
    }

    public CashRegister registerMovement(CashMovementType type, BigDecimal amount, String concept, String actorId) {
        if (status == CashRegisterStatus.CERRADA) {
            throw new CashRegisterClosedException(
                    "cash register is already closed, cannot register movements (cashRegisterId: " + cashRegisterId
                            + ")");
        }
        CashMovement movement = new CashMovement(type, amount, concept, actorId, Instant.now());
        List<CashMovement> updated = new ArrayList<>(movements);
        updated.add(movement);
        return new CashRegister(cashRegisterId, tenantId, businessDate, baseAmount, status, List.copyOf(updated),
                corrections, closedBy, closedAt, totalAmount);
    }

    public CashRegister close(String actorId, BigDecimal refundsTotal) {
        if (status == CashRegisterStatus.CERRADA) {
            throw new CashRegisterClosedException("cash register is already closed (cashRegisterId: "
                    + cashRegisterId + ")");
        }
        if (actorId == null || actorId.isBlank()) {
            throw new IllegalArgumentException("actorId is required to close a cash register");
        }
        BigDecimal total = computeTotal(refundsTotal);
        return new CashRegister(cashRegisterId, tenantId, businessDate, baseAmount, CashRegisterStatus.CERRADA,
                movements, corrections, actorId, Instant.now(), total);
    }

    public CashRegister addCorrection(String justification, String actorId) {
        if (status != CashRegisterStatus.CERRADA) {
            throw new CashRegisterNotClosedException(
                    "a correction can only be added to a closed cash register (cashRegisterId: " + cashRegisterId
                            + ")");
        }
        CashCorrection correction = new CashCorrection(justification, actorId, Instant.now());
        List<CashCorrection> updated = new ArrayList<>(corrections);
        updated.add(correction);
        return new CashRegister(cashRegisterId, tenantId, businessDate, baseAmount, status, movements,
                List.copyOf(updated), closedBy, closedAt, totalAmount);
    }

    /**
     * Total en vivo: {@code baseAmount + ingresos - pagos - gastos - refundsTotal}. Usado
     * tanto por {@link #close} (para congelar {@code totalAmount}) como por la consulta de
     * una caja `ABIERTA` (sin persistir nada).
     */
    public BigDecimal computeTotal(BigDecimal refundsTotal) {
        BigDecimal ingresos = sumByType(CashMovementType.INGRESO);
        BigDecimal pagos = sumByType(CashMovementType.PAGO);
        BigDecimal gastos = sumByType(CashMovementType.GASTO);
        BigDecimal refunds = refundsTotal == null ? BigDecimal.ZERO : refundsTotal;
        return baseAmount.add(ingresos).subtract(pagos).subtract(gastos).subtract(refunds);
    }

    private BigDecimal sumByType(CashMovementType type) {
        return movements.stream()
                .filter(movement -> movement.type() == type)
                .map(CashMovement::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public UUID cashRegisterId() {
        return cashRegisterId;
    }

    public String tenantId() {
        return tenantId;
    }

    public LocalDate businessDate() {
        return businessDate;
    }

    public BigDecimal baseAmount() {
        return baseAmount;
    }

    public CashRegisterStatus status() {
        return status;
    }

    public List<CashMovement> movements() {
        return movements;
    }

    public List<CashCorrection> corrections() {
        return corrections;
    }

    public String closedBy() {
        return closedBy;
    }

    public Instant closedAt() {
        return closedAt;
    }

    public BigDecimal totalAmount() {
        return totalAmount;
    }
}
