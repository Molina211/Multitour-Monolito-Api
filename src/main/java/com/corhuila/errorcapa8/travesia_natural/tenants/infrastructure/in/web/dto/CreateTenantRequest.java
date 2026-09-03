package com.corhuila.errorcapa8.travesia_natural.tenants.infrastructure.in.web.dto;

public record CreateTenantRequest(String tenantId, String commercialName, String actorId,
                                   AdministratorRequest administrator) {
}
