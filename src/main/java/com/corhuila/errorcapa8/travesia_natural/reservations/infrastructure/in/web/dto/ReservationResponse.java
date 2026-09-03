package com.corhuila.errorcapa8.travesia_natural.reservations.infrastructure.in.web.dto;

import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.Reservation;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ReservationResponse(UUID reservationId, UUID tenantId, String customerId, BigDecimal projectedValue,
                                   BigDecimal finalValue, BigDecimal pendingBalance, BigDecimal creditBalance,
                                   String reservationStatus, String paymentStatus, String paymentMethod,
                                   Instant createdAt) {

    public static ReservationResponse from(Reservation reservation) {
        return new ReservationResponse(
                reservation.reservationId(),
                reservation.tenantId(),
                reservation.customerId(),
                reservation.projectedValue(),
                reservation.finalValue(),
                reservation.pendingBalance(),
                reservation.creditBalance(),
                reservation.reservationStatus().label(),
                reservation.paymentStatus().label(),
                reservation.paymentMethod(),
                reservation.createdAt());
    }
}
