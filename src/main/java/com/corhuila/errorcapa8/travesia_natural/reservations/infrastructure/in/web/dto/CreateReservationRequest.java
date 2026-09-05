package com.corhuila.errorcapa8.travesia_natural.reservations.infrastructure.in.web.dto;

import java.math.BigDecimal;
import java.util.List;

public record CreateReservationRequest(BigDecimal projectedValue,
                                        List<ReservedServiceRequest> reservedServices,
                                        String holderDocument,
                                        List<CompanionRequest> companions) {
}
