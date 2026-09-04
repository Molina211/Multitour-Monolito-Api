package com.corhuila.errorcapa8.travesia_natural.reservations.infrastructure.in.web.dto;

import java.math.BigDecimal;

public record RefundReservationRequest(BigDecimal amount, String reason, String actorId, String method) {
}
