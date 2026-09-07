package com.corhuila.errorcapa8.travesia_natural.reservations.infrastructure.in.web.dto;

import java.math.BigDecimal;

public record RegisterPaymentRequest(String method, BigDecimal amount, String supportReference) {
}
