package com.corhuila.errorcapa8.travesia_natural.establishments.infrastructure.in.web.dto;

import com.corhuila.errorcapa8.travesia_natural.establishments.domain.model.AssociatedEstablishment;

import java.time.Instant;
import java.util.UUID;

public record EstablishmentResponse(UUID establishmentId, String tenantId, String kind, String name,
                                     String description, String image, boolean active, Instant createdAt) {

    public static EstablishmentResponse from(AssociatedEstablishment establishment) {
        return new EstablishmentResponse(
                establishment.establishmentId(),
                establishment.tenantId(),
                establishment.kind().name(),
                establishment.name(),
                establishment.description(),
                establishment.image(),
                establishment.active(),
                establishment.createdAt());
    }
}
