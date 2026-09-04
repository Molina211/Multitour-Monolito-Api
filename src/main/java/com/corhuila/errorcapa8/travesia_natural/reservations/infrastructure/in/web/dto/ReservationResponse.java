package com.corhuila.errorcapa8.travesia_natural.reservations.infrastructure.in.web.dto;

import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.Reservation;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ReservationResponse(UUID reservationId, String tenantId, String customerId,
                                   List<ReservedServiceResponse> reservedServices, BigDecimal projectedValue,
                                   BigDecimal finalValue, BigDecimal pendingBalance, BigDecimal creditBalance,
                                   String reservationStatus, String paymentStatus, String paymentMethod,
                                   Instant createdAt, BigDecimal pendingTransferAmount,
                                   String transferSupportReference, String cancellationReason, String cancelledBy,
                                   Instant cancelledAt) {

    public static ReservationResponse from(Reservation reservation) {
        return new ReservationResponse(
                reservation.reservationId(),
                reservation.tenantId(),
                reservation.customerId(),
                reservation.reservedServices().stream().map(ReservedServiceResponse::from).toList(),
                reservation.projectedValue(),
                reservation.finalValue(),
                reservation.pendingBalance(),
                reservation.creditBalance(),
                reservation.reservationStatus().label(),
                reservation.paymentStatus().label(),
                reservation.paymentMethod(),
                reservation.createdAt(),
                reservation.pendingTransferAmount(),
                reservation.transferSupportReference(),
                reservation.cancellationReason(),
                reservation.cancelledBy(),
                reservation.cancelledAt());
    }
}
