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

    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReservedServiceEntity> reservedServices = new ArrayList<>();

    protected ReservationEntity() {
        // JPA
    }

    public ReservationEntity(UUID reservationId, String tenantId, String customerId, BigDecimal projectedValue,
                              BigDecimal finalValue, BigDecimal pendingBalance, BigDecimal creditBalance,
                              String reservationStatus, String paymentStatus, String paymentMethod,
                              Instant createdAt, BigDecimal pendingTransferAmount, String transferSupportReference) {
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

    public List<ReservedServiceEntity> getReservedServices() {
        return reservedServices;
    }
}
