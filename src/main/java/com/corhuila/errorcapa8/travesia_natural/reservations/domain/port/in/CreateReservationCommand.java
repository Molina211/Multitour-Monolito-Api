package com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.in;

import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.ReservedService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateReservationCommand(UUID tenantId, String customerId, BigDecimal projectedValue,
                                        List<ReservedService> reservedServices) {
}
