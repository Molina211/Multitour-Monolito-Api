package com.corhuila.errorcapa8.travesia_natural.tenants.infrastructure.in.web.dto;

import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.Membership;

import java.time.Instant;
import java.util.UUID;

public record CollaboratorResponse(UUID membershipId, String tenantId, String name, String email, String role,
                                    String membershipStatus, Instant createdAt) {

    public static CollaboratorResponse from(Membership membership) {
        return new CollaboratorResponse(
                membership.membershipId(),
                membership.tenantId(),
                membership.firstName(),
                membership.email(),
                membership.role().name(),
                membership.membershipStatus().name(),
                membership.createdAt());
    }
}
