package com.corhuila.errorcapa8.travesia_natural.tenants.infrastructure.in.web.dto;

import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.Membership;

import java.time.Instant;
import java.util.UUID;

public record CustomerResponse(UUID membershipId, String tenantId, String firstName, String lastName, String email,
                                String phone, String role, String membershipStatus, Instant createdAt) {

    public static CustomerResponse from(Membership membership) {
        return new CustomerResponse(
                membership.membershipId(),
                membership.tenantId(),
                membership.firstName(),
                membership.lastName(),
                membership.email(),
                membership.phone(),
                membership.role().name(),
                membership.membershipStatus().name(),
                membership.createdAt());
    }
}
