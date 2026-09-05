package com.corhuila.errorcapa8.travesia_natural.reservations.infrastructure.out.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "reservations")
public class ReservationEntity {

    @Id
    @Column(name = "reservation_id")
    private UUID reservationId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(name = "projected_value", nullable = false)
    private BigDecimal projectedValue;

    @Column(name = "final_value", nullable = false)
    private BigDecimal finalValue;

    @Column(name = "pending_balance", nullable = false)
    private BigDecimal pendingBalance;

    @Column(name = "credit_balance", nullable = false)
    private BigDecimal creditBalance;

    @Column(name = "reservation_status", nullable = false)
    private String reservationStatus;

    @Column(name = "payment_status", nullable = false)
    private String paymentStatus;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "pending_transfer_amount")
    private BigDecimal pendingTransferAmount;

    @Column(name = "transfer_support_reference")
    private String transferSupportReference;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @Column(name = "cancelled_by")
    private String cancelledBy;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "refunded_amount")
    private BigDecimal refundedAmount;

    @Column(name = "refund_reason")
    private String refundReason;

    @Column(name = "refunded_by")
    private String refundedBy;

    @Column(name = "refund_method")
    private String refundMethod;

    @Column(name = "refunded_at")
    private Instant refundedAt;

    @Column(name = "finalized_by")
    private String finalizedBy;

    @Column(name = "finalized_at")
    private Instant finalizedAt;

    @Column(name = "holder_document")
    private String holderDocument;

    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<ReservedServiceEntity> reservedServices = new ArrayList<>();

    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<CompanionEntity> companions = new ArrayList<>();

    protected ReservationEntity() {
        // JPA
    }

    public ReservationEntity(UUID reservationId, String tenantId, String customerId, BigDecimal projectedValue,
                              BigDecimal finalValue, BigDecimal pendingBalance, BigDecimal creditBalance,
                              String reservationStatus, String paymentStatus, String paymentMethod,
                              Instant createdAt, BigDecimal pendingTransferAmount, String transferSupportReference,
                              String cancellationReason, String cancelledBy, Instant cancelledAt,
                              BigDecimal refundedAmount, String refundReason, String refundedBy,
                              String refundMethod, Instant refundedAt, String finalizedBy, Instant finalizedAt,
                              String holderDocument) {
        this.reservationId = reservationId;
        this.tenantId = tenantId;
        this.customerId = customerId;
        this.projectedValue = projectedValue;
        this.finalValue = finalValue;
        this.pendingBalance = pendingBalance;
        this.creditBalance = creditBalance;
        this.reservationStatus = reservationStatus;
        this.paymentStatus = paymentStatus;
        this.paymentMethod = paymentMethod;
        this.createdAt = createdAt;
        this.pendingTransferAmount = pendingTransferAmount;
        this.transferSupportReference = transferSupportReference;
        this.cancellationReason = cancellationReason;
        this.cancelledBy = cancelledBy;
        this.cancelledAt = cancelledAt;
        this.refundedAmount = refundedAmount;
        this.refundReason = refundReason;
        this.refundedBy = refundedBy;
        this.refundMethod = refundMethod;
        this.refundedAt = refundedAt;
        this.finalizedBy = finalizedBy;
        this.finalizedAt = finalizedAt;
        this.holderDocument = holderDocument;
    }

    /**
     * Actualiza los campos que cambian durante el ciclo de vida de una reserva ya
     * persistida (pagar, cancelar, devolver). {@code reservationId}/{@code tenantId}/
     * {@code customerId}/{@code projectedValue}/{@code createdAt}/{@code reservedServices}
     * son fijos desde la creación, no se tocan aquí.
     */
    public void updateState(BigDecimal finalValue, BigDecimal pendingBalance, BigDecimal creditBalance,
                             String reservationStatus, String paymentStatus, String paymentMethod,
                             BigDecimal pendingTransferAmount, String transferSupportReference,
                             String cancellationReason, String cancelledBy, Instant cancelledAt,
                             BigDecimal refundedAmount, String refundReason, String refundedBy,
                             String refundMethod, Instant refundedAt, String finalizedBy, Instant finalizedAt) {
        this.finalValue = finalValue;
        this.pendingBalance = pendingBalance;
        this.creditBalance = creditBalance;
        this.reservationStatus = reservationStatus;
        this.paymentStatus = paymentStatus;
        this.paymentMethod = paymentMethod;
        this.pendingTransferAmount = pendingTransferAmount;
        this.transferSupportReference = transferSupportReference;
        this.cancellationReason = cancellationReason;
        this.cancelledBy = cancelledBy;
        this.cancelledAt = cancelledAt;
        this.refundedAmount = refundedAmount;
        this.refundReason = refundReason;
        this.refundedBy = refundedBy;
        this.refundMethod = refundMethod;
        this.refundedAt = refundedAt;
        this.finalizedBy = finalizedBy;
        this.finalizedAt = finalizedAt;
    }

    public void addReservedService(ReservedServiceEntity reservedService) {
        reservedService.assignTo(this);
        this.reservedServices.add(reservedService);
    }

    public void addCompanion(CompanionEntity companion) {
        companion.assignTo(this);
        this.companions.add(companion);
    }

    public UUID getReservationId() {
        return reservationId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public BigDecimal getProjectedValue() {
        return projectedValue;
    }

    public BigDecimal getFinalValue() {
        return finalValue;
    }

    public BigDecimal getPendingBalance() {
        return pendingBalance;
    }

    public BigDecimal getCreditBalance() {
        return creditBalance;
    }

    public String getReservationStatus() {
        return reservationStatus;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public BigDecimal getPendingTransferAmount() {
        return pendingTransferAmount;
    }

    public String getTransferSupportReference() {
        return transferSupportReference;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public String getCancelledBy() {
        return cancelledBy;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public BigDecimal getRefundedAmount() {
        return refundedAmount;
    }

    public String getRefundReason() {
        return refundReason;
    }

    public String getRefundedBy() {
        return refundedBy;
    }

    public String getRefundMethod() {
        return refundMethod;
    }

    public Instant getRefundedAt() {
        return refundedAt;
    }

    public String getFinalizedBy() {
        return finalizedBy;
    }

    public Instant getFinalizedAt() {
        return finalizedAt;
    }

    public List<ReservedServiceEntity> getReservedServices() {
        return reservedServices;
    }

    public String getHolderDocument() {
        return holderDocument;
    }

    public List<CompanionEntity> getCompanions() {
        return companions;
    }
}
