package com.corhuila.errorcapa8.travesia_natural.operations.infrastructure.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reservation_executions")
public class ExecutionEntity {

    @Id
    @Column(name = "execution_id")
    private UUID executionId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "reservation_id", nullable = false, unique = true)
    private UUID reservationId;

    @Column(name = "served", nullable = false)
    private boolean served;

    @Column(name = "executed")
    private Integer executed;

    @Column(name = "causal")
    private String causal;

    @Column(name = "actor_id")
    private String actorId;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    protected ExecutionEntity() {
        // JPA
    }

    public ExecutionEntity(UUID executionId, String tenantId, UUID reservationId, boolean served, Integer executed,
                            String causal, String actorId, Instant recordedAt) {
        this.executionId = executionId;
        this.tenantId = tenantId;
        this.reservationId = reservationId;
        this.served = served;
        this.executed = executed;
        this.causal = causal;
        this.actorId = actorId;
        this.recordedAt = recordedAt;
    }

    public UUID getExecutionId() {
        return executionId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public UUID getReservationId() {
        return reservationId;
    }

    public boolean isServed() {
        return served;
    }

    public Integer getExecuted() {
        return executed;
    }

    public String getCausal() {
        return causal;
    }

    public String getActorId() {
        return actorId;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }
}
