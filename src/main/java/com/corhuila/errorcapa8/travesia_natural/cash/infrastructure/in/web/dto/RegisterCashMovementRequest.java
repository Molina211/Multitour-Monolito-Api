package com.corhuila.errorcapa8.travesia_natural.cash.infrastructure.in.web.dto;

import java.math.BigDecimal;

public record RegisterCashMovementRequest(String type, BigDecimal amount, String concept, String actorId) {
}
