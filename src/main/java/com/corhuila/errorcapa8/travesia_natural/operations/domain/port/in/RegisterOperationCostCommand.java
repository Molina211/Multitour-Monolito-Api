package com.corhuila.errorcapa8.travesia_natural.operations.domain.port.in;

import java.math.BigDecimal;
import java.util.UUID;

public record RegisterOperationCostCommand(String tenantId, UUID reservationId, String concept, BigDecimal amount,
                                            String actorId) {
}
