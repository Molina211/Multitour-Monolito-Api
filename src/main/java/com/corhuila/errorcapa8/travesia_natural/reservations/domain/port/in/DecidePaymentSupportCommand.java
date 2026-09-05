package com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.in;

import java.util.UUID;

public record DecidePaymentSupportCommand(String tenantId, UUID reservationId, String decision, String reason,
                                           String actorId) {
}
