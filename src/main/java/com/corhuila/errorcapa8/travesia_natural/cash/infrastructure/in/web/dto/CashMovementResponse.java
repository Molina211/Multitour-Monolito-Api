package com.corhuila.errorcapa8.travesia_natural.cash.infrastructure.in.web.dto;

import com.corhuila.errorcapa8.travesia_natural.cash.domain.model.CashMovement;

import java.math.BigDecimal;
import java.time.Instant;

public record CashMovementResponse(String type, BigDecimal amount, String concept, String actorId,
                                    Instant recordedAt) {

    public static CashMovementResponse from(CashMovement movement) {
        return new CashMovementResponse(movement.type().label(), movement.amount(), movement.concept(),
                movement.actorId(), movement.recordedAt());
    }
}
