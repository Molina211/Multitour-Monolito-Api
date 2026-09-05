package com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.in;

import java.util.UUID;

public record RejectRefundCommand(String tenantId, UUID reservationId, String actorId, String reason) {
}
