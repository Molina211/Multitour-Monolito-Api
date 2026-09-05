package com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.in;

import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.Companion;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.ReservedService;

import java.math.BigDecimal;
import java.util.List;

public record CreateReservationCommand(String tenantId, String customerId, BigDecimal projectedValue,
                                        List<ReservedService> reservedServices, String holderDocument,
                                        List<Companion> companions) {
}
