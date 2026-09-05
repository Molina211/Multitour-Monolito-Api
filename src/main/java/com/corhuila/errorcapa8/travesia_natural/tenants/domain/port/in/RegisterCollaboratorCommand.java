package com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.in;

public record RegisterCollaboratorCommand(String tenantId, String name, String email, String password,
                                           String actorId) {
}
