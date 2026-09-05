package com.corhuila.errorcapa8.travesia_natural.reservations.infrastructure.in.web.dto;

import java.time.LocalDate;
import java.util.UUID;

public record ReservedServiceRequest(String serviceReference, Integer partySize, LocalDate scheduledDate,
                                      UUID transportItemId) {
}
