package com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.in;

public record DeactivateTenantCommand(String tenantId, String reason, String actorId) {
}
