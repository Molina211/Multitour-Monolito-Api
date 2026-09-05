package com.corhuila.errorcapa8.travesia_natural.cash.infrastructure.out.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "cash_registers")
public class CashRegisterEntity {

    @Id
    @Column(name = "cash_register_id")
    private UUID cashRegisterId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;

    @Column(name = "base_amount", nullable = false)
    private BigDecimal baseAmount;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "closed_by")
    private String closedBy;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "total_amount")
    private BigDecimal totalAmount;

    @OneToMany(mappedBy = "cashRegister", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<CashMovementEntity> movements = new ArrayList<>();

    @OneToMany(mappedBy = "cashRegister", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<CashCorrectionEntity> corrections = new ArrayList<>();

    protected CashRegisterEntity() {
        // JPA
    }

    public CashRegisterEntity(UUID cashRegisterId, String tenantId, LocalDate businessDate, BigDecimal baseAmount,
                               String status, String closedBy, Instant closedAt, BigDecimal totalAmount) {
        this.cashRegisterId = cashRegisterId;
        this.tenantId = tenantId;
        this.businessDate = businessDate;
        this.baseAmount = baseAmount;
        this.status = status;
        this.closedBy = closedBy;
        this.closedAt = closedAt;
        this.totalAmount = totalAmount;
    }

    /**
     * Actualiza los campos que sí cambian en el ciclo de vida de una caja ya persistida
     * (cerrar, ver total congelado). {@code tenantId}/{@code businessDate}/{@code
     * baseAmount} son fijos desde la apertura, no se tocan aquí.
     */
    public void updateState(String status, String closedBy, Instant closedAt, BigDecimal totalAmount) {
        this.status = status;
        this.closedBy = closedBy;
        this.closedAt = closedAt;
        this.totalAmount = totalAmount;
    }

    public void addMovement(CashMovementEntity movement) {
        movement.assignTo(this);
        this.movements.add(movement);
    }

    public void addCorrection(CashCorrectionEntity correction) {
        correction.assignTo(this);
        this.corrections.add(correction);
    }

    public UUID getCashRegisterId() {
        return cashRegisterId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public LocalDate getBusinessDate() {
        return businessDate;
    }

    public BigDecimal getBaseAmount() {
        return baseAmount;
    }

    public String getStatus() {
        return status;
    }

    public String getClosedBy() {
        return closedBy;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public List<CashMovementEntity> getMovements() {
        return movements;
    }

    public List<CashCorrectionEntity> getCorrections() {
        return corrections;
    }
}
