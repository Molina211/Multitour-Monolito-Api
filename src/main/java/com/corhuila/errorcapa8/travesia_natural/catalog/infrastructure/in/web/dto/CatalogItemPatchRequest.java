package com.corhuila.errorcapa8.travesia_natural.catalog.infrastructure.in.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * {@code type} is deliberately not editable via PATCH: it is what decides whether
 * {@code capacity} is mandatory (RN-HOS-003). Changing it after creation would need its
 * own migration rule for existing capacity/price data, which no screen or HU asks for.
 */
public record CatalogItemPatchRequest(String name, BigDecimal price, Integer capacity, String restrictions,
                                       LocalDate validFrom, LocalDate validTo, String policy, String image,
                                       String route, BigDecimal operationalCost) {
}
