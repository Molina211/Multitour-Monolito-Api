package com.corhuila.errorcapa8.travesia_natural.catalog.infrastructure.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "catalog_items")
public class CatalogItemEntity {

    @Id
    @Column(name = "catalog_item_id")
    private UUID catalogItemId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "item_type", nullable = false)
    private String itemType;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "price", nullable = false)
    private BigDecimal price;

    @Column(name = "capacity")
    private Integer capacity;

    @Column(name = "restrictions")
    private String restrictions;

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    @Column(name = "policy")
    private String policy;

    @Column(name = "image")
    private String image;

    @Column(name = "route")
    private String route;

    @Column(name = "operational_cost")
    private BigDecimal operationalCost;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected CatalogItemEntity() {
        // JPA
    }

    public CatalogItemEntity(UUID catalogItemId, String tenantId, String itemType, String name, BigDecimal price,
                              Integer capacity, String restrictions, LocalDate validFrom, LocalDate validTo,
                              String policy, String image, String route, BigDecimal operationalCost, boolean active,
                              Instant createdAt) {
        this.catalogItemId = catalogItemId;
        this.tenantId = tenantId;
        this.itemType = itemType;
        this.name = name;
        this.price = price;
        this.capacity = capacity;
        this.restrictions = restrictions;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.policy = policy;
        this.image = image;
        this.route = route;
        this.operationalCost = operationalCost;
        this.active = active;
        this.createdAt = createdAt;
    }

    public UUID getCatalogItemId() {
        return catalogItemId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getItemType() {
        return itemType;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public String getRestrictions() {
        return restrictions;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public LocalDate getValidTo() {
        return validTo;
    }

    public String getPolicy() {
        return policy;
    }

    public String getImage() {
        return image;
    }

    public String getRoute() {
        return route;
    }

    public BigDecimal getOperationalCost() {
        return operationalCost;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
