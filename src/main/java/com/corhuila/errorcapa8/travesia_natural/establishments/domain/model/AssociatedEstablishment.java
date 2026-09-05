package com.corhuila.errorcapa8.travesia_natural.establishments.domain.model;

import com.corhuila.errorcapa8.travesia_natural.establishments.domain.exception.InvalidEstablishmentException;

import java.time.Instant;
import java.util.UUID;

/**
 * Aggregate root for the Associated Establishments bounded context (spec 025,
 * RN-ASO-001): hotels and restaurants a tenant promotes commercially, distinct from
 * {@code CatalogItem} (no price, capacity or validity window — never reserved).
 */
public final class AssociatedEstablishment {

    private final UUID establishmentId;
    private final String tenantId;
    private final EstablishmentKind kind;
    private final String name;
    private final String description;
    private final String image;
    private final boolean active;
    private final Instant createdAt;

    private AssociatedEstablishment(UUID establishmentId, String tenantId, EstablishmentKind kind, String name,
                                     String description, String image, boolean active, Instant createdAt) {
        this.establishmentId = establishmentId;
        this.tenantId = tenantId;
        this.kind = kind;
        this.name = name;
        this.description = description;
        this.image = image;
        this.active = active;
        this.createdAt = createdAt;
    }

    public static AssociatedEstablishment create(String tenantId, EstablishmentKind kind, String name,
                                                   String description, String image) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new InvalidEstablishmentException("tenantId is required");
        }
        if (kind == null) {
            throw new InvalidEstablishmentException("kind is required");
        }
        if (name == null || name.isBlank()) {
            throw new InvalidEstablishmentException("name is required");
        }

        return new AssociatedEstablishment(UUID.randomUUID(), tenantId, kind, name, description, image, true,
                Instant.now());
    }

    /**
     * Rebuilds an establishment already persisted. No invariant re-validation: data
     * already passed through {@link #create} once. Mirrors CatalogItem.reconstitute.
     */
    public static AssociatedEstablishment reconstitute(UUID establishmentId, String tenantId, EstablishmentKind kind,
                                                         String name, String description, String image,
                                                         boolean active, Instant createdAt) {
        return new AssociatedEstablishment(establishmentId, tenantId, kind, name, description, image, active,
                createdAt);
    }

    /**
     * Deactivates the establishment (RN-ASO-001: "conservando su información e
     * histórico") — the row is never deleted.
     */
    public AssociatedEstablishment deactivate() {
        if (!active) {
            throw new InvalidEstablishmentException("establishment is already inactive: " + establishmentId);
        }
        return new AssociatedEstablishment(establishmentId, tenantId, kind, name, description, image, false,
                createdAt);
    }

    public AssociatedEstablishment reactivate() {
        if (active) {
            throw new InvalidEstablishmentException("establishment is already active: " + establishmentId);
        }
        return new AssociatedEstablishment(establishmentId, tenantId, kind, name, description, image, true,
                createdAt);
    }

    public UUID establishmentId() {
        return establishmentId;
    }

    public String tenantId() {
        return tenantId;
    }

    public EstablishmentKind kind() {
        return kind;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public String image() {
        return image;
    }

    public boolean active() {
        return active;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
