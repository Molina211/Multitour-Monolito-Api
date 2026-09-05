package com.corhuila.errorcapa8.travesia_natural.tenants.infrastructure.in.web.dto;

import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.Tenant;

import java.time.Instant;

public record TenantResponse(String tenantId, String commercialName, String tenantStatus, Instant createdAt,
                              boolean allowCollaboratorSupportValidation) {

    public static TenantResponse from(Tenant tenant) {
        return new TenantResponse(
                tenant.tenantId(),
                tenant.commercialName(),
                tenant.tenantStatus().name(),
                tenant.createdAt(),
                tenant.allowCollaboratorSupportValidation());
    }
}
