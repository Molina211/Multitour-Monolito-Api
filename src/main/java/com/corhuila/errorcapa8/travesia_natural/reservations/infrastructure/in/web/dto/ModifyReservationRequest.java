package com.corhuila.errorcapa8.travesia_natural.reservations.infrastructure.in.web.dto;

import java.math.BigDecimal;
import java.util.List;

public record ModifyReservationRequest(List<ReservedServiceRequest> reservedServices, BigDecimal projectedValue,
                                        BigDecimal finalValue, String reason, String actorId) {
}
