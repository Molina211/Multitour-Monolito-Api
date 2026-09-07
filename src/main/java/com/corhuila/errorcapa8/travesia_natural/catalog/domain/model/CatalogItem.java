package com.corhuila.errorcapa8.travesia_natural.catalog.domain.model;

import com.corhuila.errorcapa8.travesia_natural.catalog.domain.exception.InvalidCatalogItemException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Aggregate root for the Operational Catalog bounded context (spec 005, HU-CAT-001).
 * A single entity covers TOUR, LODGING, FOOD and TRANSPORT (discriminated by
 * {@code type}) instead of near-identical classes — same criterion already used for
 * Membership's roles. TRANSPORT was added in spec 015 once the Frontend shipped real
 * screens for it; {@code route}/{@code operationalCost} only apply to that type, but
 * are left unvalidated for the others (nobody requires that guard yet).
 */
public final class CatalogItem {

    private final UUID catalogItemId;
    private final String tenantId;
    private final CatalogItemType type;
    private final String name;
    private final BigDecimal price;
    private final Integer capacity;
    private final String restrictions;
    private final LocalDate validFrom;
    private final LocalDate validTo;
    private final String policy;
    private final String image;
    private final String route;
    private final BigDecimal operationalCost;
    private final boolean active;
    private final Instant createdAt;

    private CatalogItem(UUID catalogItemId, String tenantId, CatalogItemType type, String name, BigDecimal price,
                         Integer capacity, String restrictions, LocalDate validFrom, LocalDate validTo,
                         String policy, String image, String route, BigDecimal operationalCost, boolean active,
                         Instant createdAt) {
        this.catalogItemId = catalogItemId;
        this.tenantId = tenantId;
        this.type = type;
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

    /**
     * Creates a catalog item in active status. {@code capacity} is required and must be
     * positive when {@code type == LODGING} (RN-HOS-003: lodging must have at minimum price
     * and capacity information); for TOUR/FOOD it is simply not applicable and stays null.
     */
    public static CatalogItem create(String tenantId, CatalogItemType type, String name, BigDecimal price,
                                      Integer capacity, String restrictions, LocalDate validFrom, LocalDate validTo,
                                      String policy, String image, String route, BigDecimal operationalCost) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new InvalidCatalogItemException("tenantId is required");
        }
        if (type == null) {
            throw new InvalidCatalogItemException("type is required");
        }
        if (name == null || name.isBlank()) {
            throw new InvalidCatalogItemException("name is required");
        }
        if (price == null) {
            throw new InvalidCatalogItemException("price is required");
        }
        validateCapacity(type, capacity);

        return new CatalogItem(UUID.randomUUID(), tenantId, type, name, price, capacity, restrictions, validFrom,
                validTo, policy, image, route, operationalCost, true, Instant.now());
    }

    /**
     * Rebuilds a catalog item already persisted. No invariant re-validation: data already
     * passed through {@link #create} once. Mirrors Tenant.reconstitute/Membership.reconstitute.
     */
    public static CatalogItem reconstitute(UUID catalogItemId, String tenantId, CatalogItemType type, String name,
                                            BigDecimal price, Integer capacity, String restrictions,
                                            LocalDate validFrom, LocalDate validTo, String policy, String image,
                                            String route, BigDecimal operationalCost, boolean active,
                                            Instant createdAt) {
        return new CatalogItem(catalogItemId, tenantId, type, name, price, capacity, restrictions, validFrom,
                validTo, policy, image, route, operationalCost, active, createdAt);
    }

    /**
     * Applies a partial update (PATCH semantics): any parameter left {@code null} keeps its
     * current value. The result is re-validated exactly like {@link #create} (e.g. a PATCH
     * that clears the capacity of a LODGING item is rejected).
     */
    public CatalogItem update(String name, BigDecimal price, Integer capacity, String restrictions,
                               LocalDate validFrom, LocalDate validTo, String policy, String image, String route,
                               BigDecimal operationalCost) {
        String newName = name != null ? name : this.name;
        BigDecimal newPrice = price != null ? price : this.price;
        Integer newCapacity = capacity != null ? capacity : this.capacity;
        String newRestrictions = restrictions != null ? restrictions : this.restrictions;
        LocalDate newValidFrom = validFrom != null ? validFrom : this.validFrom;
        LocalDate newValidTo = validTo != null ? validTo : this.validTo;
        String newPolicy = policy != null ? policy : this.policy;
        String newImage = image != null ? image : this.image;
        String newRoute = route != null ? route : this.route;
        BigDecimal newOperationalCost = operationalCost != null ? operationalCost : this.operationalCost;

        if (newName.isBlank()) {
            throw new InvalidCatalogItemException("name is required");
        }
        validateCapacity(type, newCapacity);

        return new CatalogItem(catalogItemId, tenantId, type, newName, newPrice, newCapacity, newRestrictions,
                newValidFrom, newValidTo, newPolicy, newImage, newRoute, newOperationalCost, active, createdAt);
    }

    /**
     * Deactivates the item (HU-CAT-001 escenario 1): stops it from being offered in new
     * reservations, but the row is never deleted — preserves historical traceability.
     */
    public CatalogItem deactivate() {
        if (!active) {
            throw new InvalidCatalogItemException("catalog item is already inactive: " + catalogItemId);
        }
        return new CatalogItem(catalogItemId, tenantId, type, name, price, capacity, restrictions, validFrom,
                validTo, policy, image, route, operationalCost, false, createdAt);
    }

    /**
     * Reactivates the item. Only reachable through this explicit operation, never an implicit
     * side effect of another action (same criterion as Tenant.reactivate, INV-TEN-002).
     */
    public CatalogItem reactivate() {
        if (active) {
            throw new InvalidCatalogItemException("catalog item is already active: " + catalogItemId);
        }
        return new CatalogItem(catalogItemId, tenantId, type, name, price, capacity, restrictions, validFrom,
                validTo, policy, image, route, operationalCost, true, createdAt);
    }

    private static void validateCapacity(CatalogItemType type, Integer capacity) {
        if (type == CatalogItemType.LODGING && (capacity == null || capacity <= 0)) {
            throw new InvalidCatalogItemException("capacity is required and must be positive for LODGING items");
        }
    }

    public UUID catalogItemId() {
        return catalogItemId;
    }

    public String tenantId() {
        return tenantId;
    }

    public CatalogItemType type() {
        return type;
    }

    public String name() {
        return name;
    }

    public BigDecimal price() {
        return price;
    }

    public Integer capacity() {
        return capacity;
    }

    public String restrictions() {
        return restrictions;
    }

    public LocalDate validFrom() {
        return validFrom;
    }

    public LocalDate validTo() {
        return validTo;
    }

    public String policy() {
        return policy;
    }

    public String image() {
        return image;
    }

    public String route() {
        return route;
    }

    public BigDecimal operationalCost() {
        return operationalCost;
    }

    public boolean active() {
        return active;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
