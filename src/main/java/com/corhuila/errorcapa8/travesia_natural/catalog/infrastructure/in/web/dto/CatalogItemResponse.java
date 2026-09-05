package com.corhuila.errorcapa8.travesia_natural.catalog.infrastructure.in.web.dto;

import com.corhuila.errorcapa8.travesia_natural.catalog.domain.model.CatalogItem;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CatalogItemResponse(UUID catalogItemId, String tenantId, String type, String name, BigDecimal price,
                                   Integer capacity, String restrictions, LocalDate validFrom, LocalDate validTo,
                                   String policy, String image, String route, BigDecimal operationalCost,
                                   boolean active, Instant createdAt) {

    public static CatalogItemResponse from(CatalogItem catalogItem) {
        return new CatalogItemResponse(
                catalogItem.catalogItemId(),
                catalogItem.tenantId(),
                catalogItem.type().name(),
                catalogItem.name(),
                catalogItem.price(),
                catalogItem.capacity(),
                catalogItem.restrictions(),
                catalogItem.validFrom(),
                catalogItem.validTo(),
                catalogItem.policy(),
                catalogItem.image(),
                catalogItem.route(),
                catalogItem.operationalCost(),
                catalogItem.active(),
                catalogItem.createdAt());
    }
}
