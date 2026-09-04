package com.corhuila.errorcapa8.travesia_natural.operations.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Costo operacional registrado sobre una reserva en ejecución (spec 010, RF-009): puede
 * haber varios por reserva, a diferencia de {@link Execution} que es único.
 */
public final class OperationCost {

    private final UUID costId;
    private final String tenantId;
    private final UUID reservationId;
    private final String concept;
    private final BigDecimal amount;
    private final String actorId;
    private final Instant recordedAt;

    private OperationCost(UUID costId, String tenantId, UUID reservationId, String concept, BigDecimal amount,
                           String actorId, Instant recordedAt) {
        this.costId = costId;
        this.tenantId = tenantId;
        this.reservationId = reservationId;
        this.concept = concept;
        this.amount = amount;
        this.actorId = actorId;
        this.recordedAt = recordedAt;
    }

    public static OperationCost create(String tenantId, UUID reservationId, String concept, BigDecimal amount,
                                        String actorId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId is required");
        }
        if (reservationId == null) {
            throw new IllegalArgumentException("reservationId is required");
        }
        if (concept == null || concept.isBlank()) {
            throw new IllegalArgumentException("concept is required");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be a positive value");
        }

        return new OperationCost(UUID.randomUUID(), tenantId, reservationId, concept, amount, actorId,
                Instant.now());
    }

    public static OperationCost reconstitute(UUID costId, String tenantId, UUID reservationId, String concept,
                                              BigDecimal amount, String actorId, Instant recordedAt) {
        return new OperationCost(costId, tenantId, reservationId, concept, amount, actorId, recordedAt);
    }

    public UUID costId() {
        return costId;
    }

    public String tenantId() {
        return tenantId;
    }

    public UUID reservationId() {
        return reservationId;
    }

    public String concept() {
        return concept;
    }

    public BigDecimal amount() {
        return amount;
    }

    public String actorId() {
        return actorId;
    }

    public Instant recordedAt() {
        return recordedAt;
    }
}
