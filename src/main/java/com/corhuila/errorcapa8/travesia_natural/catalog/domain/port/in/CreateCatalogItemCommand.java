package com.corhuila.errorcapa8.travesia_natural.catalog.domain.port.in;

import com.corhuila.errorcapa8.travesia_natural.catalog.domain.model.CatalogItemType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateCatalogItemCommand(String tenantId, CatalogItemType type, String name, BigDecimal price,
                                        Integer capacity, String restrictions, LocalDate validFrom,
                                        LocalDate validTo, String policy, String image, String route,
                                        BigDecimal operationalCost) {
}
