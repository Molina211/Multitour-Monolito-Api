package com.corhuila.errorcapa8.travesia_natural.discounts.domain.port.in;

import com.corhuila.errorcapa8.travesia_natural.discounts.domain.model.DiscountBase;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateDiscountCommand(String tenantId, UUID catalogItemId, int percentage, LocalDate validFrom,
                                     LocalDate validTo, int priority, boolean stackable, BigDecimal cap,
                                     DiscountBase base) {
}
