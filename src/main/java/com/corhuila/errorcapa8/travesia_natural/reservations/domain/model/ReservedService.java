package com.corhuila.errorcapa8.travesia_natural.reservations.domain.model;

import java.time.LocalDate;

public record ReservedService(String serviceReference, Integer partySize, LocalDate scheduledDate) {

    public ReservedService {
        if (serviceReference == null || serviceReference.isBlank()) {
            throw new IllegalArgumentException("serviceReference is required");
        }
    }
}
