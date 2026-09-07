package com.corhuila.errorcapa8.travesia_natural.cash.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

public record CashMovement(CashMovementType type, BigDecimal amount, String concept, String actorId,
                            Instant recordedAt) {

    public CashMovement {
        if (type == null) {
            throw new IllegalArgumentException("movement type is required");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("movement amount must be a positive value");
        }
        if (concept == null || concept.isBlank()) {
            throw new IllegalArgumentException("movement concept is required");
        }
        if (actorId == null || actorId.isBlank()) {
            throw new IllegalArgumentException("movement actorId is required");
        }
    }
}
