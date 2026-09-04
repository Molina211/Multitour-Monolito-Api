package com.corhuila.errorcapa8.travesia_natural.operations.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Registro de ejecución real de una reserva (spec 010, RF-007): uno por reserva, creado
 * una sola vez cuando `Reservation.startExecution()` transiciona a `EN_EJECUCION`.
 */
public final class Execution {

    private final UUID executionId;
    private final String tenantId;
    private final UUID reservationId;
    private final boolean served;
    private final Integer executed;
    private final String causal;
    private final String actorId;
    private final Instant recordedAt;

    private Execution(UUID executionId, String tenantId, UUID reservationId, boolean served, Integer executed,
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

    /**
     * `causal` es obligatoria cuando {@code served == false} (RN-EJE-002): explica por qué
     * no se prestó el servicio. Cuando {@code served == true}, `causal` no aplica.
     */
    public static Execution create(String tenantId, UUID reservationId, boolean served, Integer executed,
                                    String causal, String actorId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId is required");
        }
        if (reservationId == null) {
            throw new IllegalArgumentException("reservationId is required");
        }
        if (!served && (causal == null || causal.isBlank())) {
            throw new IllegalArgumentException("causal is required when the service was not served");
        }

        return new Execution(UUID.randomUUID(), tenantId, reservationId, served, served ? executed : null,
                served ? null : causal, actorId, Instant.now());
    }

    public static Execution reconstitute(UUID executionId, String tenantId, UUID reservationId, boolean served,
                                          Integer executed, String causal, String actorId, Instant recordedAt) {
        return new Execution(executionId, tenantId, reservationId, served, executed, causal, actorId, recordedAt);
    }

    public UUID executionId() {
        return executionId;
    }

    public String tenantId() {
        return tenantId;
    }

    public UUID reservationId() {
        return reservationId;
    }

    public boolean served() {
        return served;
    }

    public Integer executed() {
        return executed;
    }

    public String causal() {
        return causal;
    }

    public String actorId() {
        return actorId;
    }

    public Instant recordedAt() {
        return recordedAt;
    }
}
