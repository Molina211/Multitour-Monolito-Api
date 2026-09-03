package com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.in;

public record ReactivateTenantCommand(String tenantId, String reason, String actorId) {
}
