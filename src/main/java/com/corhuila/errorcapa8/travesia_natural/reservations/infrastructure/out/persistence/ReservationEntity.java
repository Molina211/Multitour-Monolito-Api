package com.corhuila.errorcapa8.travesia_natural.reservations.infrastructure.out.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
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

    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReservedServiceEntity> reservedServices = new ArrayList<>();

    protected ReservationEntity() {
        // JPA
    }

    public ReservationEntity(UUID reservationId, String tenantId, String customerId, BigDecimal projectedValue,
                              BigDecimal finalValue, BigDecimal pendingBalance, BigDecimal creditBalance,
                              String reservationStatus, String paymentStatus, String paymentMethod,
                              Instant createdAt, BigDecimal pendingTransferAmount, String transferSupportReference,
                              String cancellationReason, String cancelledBy, Instant cancelledAt,
                              BigDecimal refundedAmount, String refundReason, String refundedBy,
                              String refundMethod, Instant refundedAt) {
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
    }

    public void addReservedService(ReservedServiceEntity reservedService) {
        reservedService.assignTo(this);
        this.reservedServices.add(reservedService);
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

    public List<ReservedServiceEntity> getReservedServices() {
        return reservedServices;
    }
}
