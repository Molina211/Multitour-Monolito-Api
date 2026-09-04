package com.corhuila.errorcapa8.travesia_natural.tenants.infrastructure.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "tenants")
public class TenantEntity {

    @Id
    @Column(name = "tenant_id")
    private String tenantId;

    @Column(name = "commercial_name", nullable = false)
    private String commercialName;

    @Column(name = "tenant_status", nullable = false)
    private String tenantStatus;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected TenantEntity() {
        // JPA
    }

    public TenantEntity(String tenantId, String commercialName, String tenantStatus, Instant createdAt) {
        this.tenantId = tenantId;
        this.commercialName = commercialName;
        this.tenantStatus = tenantStatus;
        this.createdAt = createdAt;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getCommercialName() {
        return commercialName;
    }

    public String getTenantStatus() {
        return tenantStatus;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
