package com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.in;

import java.util.UUID;

public record ApplyDiscountCommand(String tenantId, UUID reservationId, Integer percentage, String reason,
                                    String actorId) {
}
