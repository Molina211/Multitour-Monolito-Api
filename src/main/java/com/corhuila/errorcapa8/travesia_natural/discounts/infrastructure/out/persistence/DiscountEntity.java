package com.corhuila.errorcapa8.travesia_natural.discounts.infrastructure.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "discounts")
public class DiscountEntity {

    @Id
    @Column(name = "discount_id")
    private UUID discountId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "catalog_item_id", nullable = false)
    private UUID catalogItemId;

    @Column(name = "percentage", nullable = false)
    private int percentage;

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    @Column(name = "priority", nullable = false)
    private int priority;

    @Column(name = "stackable", nullable = false)
    private boolean stackable;

    @Column(name = "cap")
    private BigDecimal cap;

    @Column(name = "base", nullable = false)
    private String base;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected DiscountEntity() {
        // JPA
    }

    public DiscountEntity(UUID discountId, String tenantId, UUID catalogItemId, int percentage, LocalDate validFrom,
                           LocalDate validTo, int priority, boolean stackable, BigDecimal cap, String base,
                           boolean active, Instant createdAt) {
        this.discountId = discountId;
        this.tenantId = tenantId;
        this.catalogItemId = catalogItemId;
        this.percentage = percentage;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.priority = priority;
        this.stackable = stackable;
        this.cap = cap;
        this.base = base;
        this.active = active;
        this.createdAt = createdAt;
    }

    public UUID getDiscountId() {
        return discountId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public UUID getCatalogItemId() {
        return catalogItemId;
    }

    public int getPercentage() {
        return percentage;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public LocalDate getValidTo() {
        return validTo;
    }

    public int getPriority() {
        return priority;
    }

    public boolean isStackable() {
        return stackable;
    }

    public BigDecimal getCap() {
        return cap;
    }

    public String getBase() {
        return base;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
