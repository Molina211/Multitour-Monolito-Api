package com.corhuila.errorcapa8.travesia_natural.reservations.infrastructure.in.web.dto;

import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.ReservedService;

import java.time.LocalDate;

public record ReservedServiceResponse(String serviceReference, Integer partySize, LocalDate scheduledDate) {

    public static ReservedServiceResponse from(ReservedService reservedService) {
        return new ReservedServiceResponse(
                reservedService.serviceReference(),
                reservedService.partySize(),
                reservedService.scheduledDate());
    }
}
