package com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.in;

import java.math.BigDecimal;
import java.util.UUID;

public record RefundReservationCommand(String tenantId, UUID reservationId, BigDecimal amount, String reason,
                                        String actorId, String method) {
}
