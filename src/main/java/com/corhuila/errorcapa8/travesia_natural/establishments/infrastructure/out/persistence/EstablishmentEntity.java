package com.corhuila.errorcapa8.travesia_natural.establishments.infrastructure.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "establishments")
public class EstablishmentEntity {

    @Id
    @Column(name = "establishment_id")
    private UUID establishmentId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "kind", nullable = false)
    private String kind;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "image")
    private String image;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected EstablishmentEntity() {
        // JPA
    }

    public EstablishmentEntity(UUID establishmentId, String tenantId, String kind, String name, String description,
                                String image, boolean active, Instant createdAt) {
        this.establishmentId = establishmentId;
        this.tenantId = tenantId;
        this.kind = kind;
        this.name = name;
        this.description = description;
        this.image = image;
        this.active = active;
        this.createdAt = createdAt;
    }

    public UUID getEstablishmentId() {
        return establishmentId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getKind() {
        return kind;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getImage() {
        return image;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
