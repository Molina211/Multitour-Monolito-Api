package com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.in;

public record UpdateCollaboratorSupportValidationPermissionCommand(String tenantId, String actorId, boolean allow) {
}
