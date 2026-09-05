package com.corhuila.errorcapa8.travesia_natural.reservations.infrastructure.in.web.dto;

import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.ReservedService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ReservedServiceResponse(String serviceReference, Integer partySize, LocalDate scheduledDate,
                                       UUID transportItemId, BigDecimal transportCost) {

    public static ReservedServiceResponse from(ReservedService reservedService) {
        return new ReservedServiceResponse(
                reservedService.serviceReference(),
                reservedService.partySize(),
                reservedService.scheduledDate(),
                reservedService.transportItemId(),
                reservedService.transportCost());
    }
}
