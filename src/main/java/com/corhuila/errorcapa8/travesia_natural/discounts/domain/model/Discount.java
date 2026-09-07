package com.corhuila.errorcapa8.travesia_natural.discounts.domain.model;

import com.corhuila.errorcapa8.travesia_natural.discounts.domain.exception.InvalidDiscountException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Aggregate root for the Discounts bounded context (spec 008, RF-005A/RF-005B partial —
 * see spec.md "Fuera de alcance"). Persists a discount rule tied to one {@code
 * catalogItemId}; deliberately does not validate overlapping validity windows against
 * other discounts of the same item — the PDR allows simultaneous discounts and leaves
 * their combination (order, stacking, cap, base) to the actual application logic, which
 * is out of scope here.
 */
public final class Discount {

    private final UUID discountId;
    private final String tenantId;
    private final UUID catalogItemId;
    private final int percentage;
    private final LocalDate validFrom;
    private final LocalDate validTo;
    private final int priority;
    private final boolean stackable;
    private final BigDecimal cap;
    private final DiscountBase base;
    private final boolean active;
    private final Instant createdAt;

    private Discount(UUID discountId, String tenantId, UUID catalogItemId, int percentage, LocalDate validFrom,
                      LocalDate validTo, int priority, boolean stackable, BigDecimal cap, DiscountBase base,
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

    public static Discount create(String tenantId, UUID catalogItemId, int percentage, LocalDate validFrom,
                                   LocalDate validTo, int priority, boolean stackable, BigDecimal cap,
                                   DiscountBase base) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new InvalidDiscountException("tenantId is required");
        }
        if (catalogItemId == null) {
            throw new InvalidDiscountException("catalogItemId is required");
        }
        if (base == null) {
            throw new InvalidDiscountException("base is required");
        }
        validatePercentage(percentage);
        validateValidity(validFrom, validTo);
        validateCap(cap);

        return new Discount(UUID.randomUUID(), tenantId, catalogItemId, percentage, validFrom, validTo, priority,
                stackable, cap, base, true, Instant.now());
    }

    /**
     * Rebuilds a discount already persisted. No invariant re-validation, same criterion
     * as {@code CatalogItem.reconstitute}.
     */
    public static Discount reconstitute(UUID discountId, String tenantId, UUID catalogItemId, int percentage,
                                         LocalDate validFrom, LocalDate validTo, int priority, boolean stackable,
                                         BigDecimal cap, DiscountBase base, boolean active, Instant createdAt) {
        return new Discount(discountId, tenantId, catalogItemId, percentage, validFrom, validTo, priority, stackable,
                cap, base, active, createdAt);
    }

    /**
     * Applies a partial update (PATCH semantics): any parameter left {@code null} keeps
     * its current value. {@code percentage}/{@code priority}/{@code stackable} use their
     * boxed types here only to distinguish "not sent" from a real value; the result is
     * re-validated exactly like {@link #create}.
     */
    public Discount update(Integer percentage, LocalDate validFrom, LocalDate validTo, Integer priority,
                            Boolean stackable, BigDecimal cap, DiscountBase base) {
        int newPercentage = percentage != null ? percentage : this.percentage;
        LocalDate newValidFrom = validFrom != null ? validFrom : this.validFrom;
        LocalDate newValidTo = validTo != null ? validTo : this.validTo;
        int newPriority = priority != null ? priority : this.priority;
        boolean newStackable = stackable != null ? stackable : this.stackable;
        BigDecimal newCap = cap != null ? cap : this.cap;
        DiscountBase newBase = base != null ? base : this.base;

        validatePercentage(newPercentage);
        validateValidity(newValidFrom, newValidTo);
        validateCap(newCap);

        return new Discount(discountId, tenantId, catalogItemId, newPercentage, newValidFrom, newValidTo,
                newPriority, newStackable, newCap, newBase, active, createdAt);
    }

    /**
     * Deactivates the discount: stops it from applying to new charges, but the row is
     * never deleted — same traceability criterion as {@code CatalogItem.deactivate}.
     */
    public Discount deactivate() {
        if (!active) {
            throw new InvalidDiscountException("discount is already inactive: " + discountId);
        }
        return new Discount(discountId, tenantId, catalogItemId, percentage, validFrom, validTo, priority, stackable,
                cap, base, false, createdAt);
    }

    public Discount reactivate() {
        if (active) {
            throw new InvalidDiscountException("discount is already active: " + discountId);
        }
        return new Discount(discountId, tenantId, catalogItemId, percentage, validFrom, validTo, priority, stackable,
                cap, base, true, createdAt);
    }

    private static void validatePercentage(int percentage) {
        if (percentage < 1 || percentage > 100) {
            throw new InvalidDiscountException("percentage must be between 1 and 100");
        }
    }

    private static void validateValidity(LocalDate validFrom, LocalDate validTo) {
        if (validFrom != null && validTo != null && validTo.isBefore(validFrom)) {
            throw new InvalidDiscountException("validTo cannot be before validFrom");
        }
    }

    private static void validateCap(BigDecimal cap) {
        if (cap != null && cap.signum() <= 0) {
            throw new InvalidDiscountException("cap must be positive when present");
        }
    }

    public UUID discountId() {
        return discountId;
    }

    public String tenantId() {
        return tenantId;
    }

    public UUID catalogItemId() {
        return catalogItemId;
    }

    public int percentage() {
        return percentage;
    }

    public LocalDate validFrom() {
        return validFrom;
    }

    public LocalDate validTo() {
        return validTo;
    }

    public int priority() {
        return priority;
    }

    public boolean stackable() {
        return stackable;
    }

    public BigDecimal cap() {
        return cap;
    }

    public DiscountBase base() {
        return base;
    }

    public boolean active() {
        return active;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
