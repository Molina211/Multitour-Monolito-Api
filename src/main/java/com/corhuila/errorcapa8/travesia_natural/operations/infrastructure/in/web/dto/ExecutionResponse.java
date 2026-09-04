package com.corhuila.errorcapa8.travesia_natural.operations.infrastructure.in.web.dto;

import com.corhuila.errorcapa8.travesia_natural.operations.domain.model.Execution;

import java.time.Instant;
import java.util.UUID;

public record ExecutionResponse(UUID reservationId, boolean served, Integer executed, String causal, String actorId,
                                 Instant recordedAt) {

    public static ExecutionResponse from(Execution execution) {
        return new ExecutionResponse(execution.reservationId(), execution.served(), execution.executed(),
                execution.causal(), execution.actorId(), execution.recordedAt());
    }
}
