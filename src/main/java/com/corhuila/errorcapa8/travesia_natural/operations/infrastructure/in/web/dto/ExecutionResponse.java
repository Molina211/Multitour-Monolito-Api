package com.corhuila.errorcapa8.travesia_natural.operations.infrastructure.in.web.dto;

import com.corhuila.errorcapa8.travesia_natural.operations.domain.model.Execution;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.Reservation;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.ReservationStatus;

import java.time.Instant;
import java.util.UUID;

public record ExecutionResponse(UUID reservationId, boolean served, Integer executed, String causal, String actorId,
                                 Instant recordedAt, boolean finalized, String finalizedBy, Instant finalizedAt) {

    public static ExecutionResponse from(Execution execution, Reservation reservation) {
        boolean finalized = reservation.reservationStatus() == ReservationStatus.FINALIZADA;
        return new ExecutionResponse(execution.reservationId(), execution.served(), execution.executed(),
                execution.causal(), execution.actorId(), execution.recordedAt(), finalized,
                reservation.finalizedBy(), reservation.finalizedAt());
    }

    /**
     * Recién registrada la ejecución (spec 010), la reserva queda en `EnEjecucion`: no puede
     * estar finalizada todavía, así que no hace falta cargar la `Reservation` aquí.
     */
    public static ExecutionResponse from(Execution execution) {
        return new ExecutionResponse(execution.reservationId(), execution.served(), execution.executed(),
                execution.causal(), execution.actorId(), execution.recordedAt(), false, null, null);
    }
}
