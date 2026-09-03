package com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.in;

public record CreateTenantCommand(String tenantId, String commercialName, String administratorEmail,
                                   String administratorPassword) {
}
