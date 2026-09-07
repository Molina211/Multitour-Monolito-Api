package com.corhuila.errorcapa8.travesia_natural.reservations.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ReservedService(String serviceReference, Integer partySize, LocalDate scheduledDate,
                               UUID transportItemId, BigDecimal transportCost) {

    public ReservedService {
        if (serviceReference == null || serviceReference.isBlank()) {
            throw new IllegalArgumentException("serviceReference is required");
        }
    }
}
