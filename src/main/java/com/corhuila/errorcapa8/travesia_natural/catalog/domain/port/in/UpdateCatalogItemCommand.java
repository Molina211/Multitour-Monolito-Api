package com.corhuila.errorcapa8.travesia_natural.catalog.domain.port.in;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record UpdateCatalogItemCommand(String tenantId, UUID catalogItemId, String name, BigDecimal price,
                                        Integer capacity, String restrictions, LocalDate validFrom,
                                        LocalDate validTo, String policy, String image, String route,
                                        BigDecimal operationalCost) {
}
