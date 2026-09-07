package com.corhuila.errorcapa8.travesia_natural.cash.domain.port.in;

import com.corhuila.errorcapa8.travesia_natural.cash.domain.model.CashMovementType;

import java.math.BigDecimal;
import java.util.UUID;

public record RegisterCashMovementCommand(String tenantId, UUID cashRegisterId, CashMovementType type,
                                           BigDecimal amount, String concept, String actorId) {
}
