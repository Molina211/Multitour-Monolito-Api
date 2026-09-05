package com.corhuila.errorcapa8.travesia_natural.cash.domain.model;

import java.time.Instant;

public record CashCorrection(String justification, String appliedBy, Instant appliedAt) {

    public CashCorrection {
        if (justification == null || justification.isBlank()) {
            throw new IllegalArgumentException("correction justification is required");
        }
        if (appliedBy == null || appliedBy.isBlank()) {
            throw new IllegalArgumentException("correction appliedBy is required");
        }
    }
}
