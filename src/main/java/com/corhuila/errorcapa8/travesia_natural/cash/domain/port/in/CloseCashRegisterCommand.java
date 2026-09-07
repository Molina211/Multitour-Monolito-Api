package com.corhuila.errorcapa8.travesia_natural.cash.domain.port.in;

import java.util.UUID;

public record CloseCashRegisterCommand(String tenantId, UUID cashRegisterId, String actorId) {
}
