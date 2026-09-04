package com.corhuila.errorcapa8.travesia_natural.operations.infrastructure.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "operation_costs")
public class OperationCostEntity {

    @Id
    @Column(name = "cost_id")
    private UUID costId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "reservation_id", nullable = false)
    private UUID reservationId;

    @Column(name = "concept", nullable = false)
    private String concept;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "actor_id")
    private String actorId;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    protected OperationCostEntity() {
        // JPA
    }

    public OperationCostEntity(UUID costId, String tenantId, UUID reservationId, String concept, BigDecimal amount,
                                String actorId, Instant recordedAt) {
        this.costId = costId;
        this.tenantId = tenantId;
        this.reservationId = reservationId;
        this.concept = concept;
        this.amount = amount;
        this.actorId = actorId;
        this.recordedAt = recordedAt;
    }

    public UUID getCostId() {
        return costId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public UUID getReservationId() {
        return reservationId;
    }

    public String getConcept() {
        return concept;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getActorId() {
        return actorId;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }
}
