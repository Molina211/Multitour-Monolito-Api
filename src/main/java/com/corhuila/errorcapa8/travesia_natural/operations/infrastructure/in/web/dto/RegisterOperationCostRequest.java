package com.corhuila.errorcapa8.travesia_natural.operations.infrastructure.in.web.dto;

import java.math.BigDecimal;

public record RegisterOperationCostRequest(String concept, BigDecimal amount, String actorId) {
}
