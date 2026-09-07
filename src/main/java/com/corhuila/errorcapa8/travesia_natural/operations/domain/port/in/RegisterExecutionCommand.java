package com.corhuila.errorcapa8.travesia_natural.operations.domain.port.in;

import java.util.UUID;

public record RegisterExecutionCommand(String tenantId, UUID reservationId, boolean served, Integer executed,
                                        String causal, String actorId) {
}
