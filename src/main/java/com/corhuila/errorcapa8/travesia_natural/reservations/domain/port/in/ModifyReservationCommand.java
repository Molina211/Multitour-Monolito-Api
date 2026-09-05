package com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.in;

import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.ReservedService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ModifyReservationCommand(String tenantId, UUID reservationId, List<ReservedService> reservedServices,
                                        BigDecimal projectedValue, BigDecimal finalValue, String reason,
                                        String actorId) {
}
