package com.corhuila.errorcapa8.travesia_natural.operations.infrastructure.in.web.dto;

import com.corhuila.errorcapa8.travesia_natural.operations.domain.model.OperationCost;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OperationCostResponse(UUID costId, UUID reservationId, String concept, BigDecimal amount,
                                     String actorId, Instant recordedAt) {

    public static OperationCostResponse from(OperationCost operationCost) {
        return new OperationCostResponse(operationCost.costId(), operationCost.reservationId(),
                operationCost.concept(), operationCost.amount(), operationCost.actorId(),
                operationCost.recordedAt());
    }
}
