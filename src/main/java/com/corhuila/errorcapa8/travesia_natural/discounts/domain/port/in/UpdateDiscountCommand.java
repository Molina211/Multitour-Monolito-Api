package com.corhuila.errorcapa8.travesia_natural.discounts.domain.port.in;

import com.corhuila.errorcapa8.travesia_natural.discounts.domain.model.DiscountBase;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record UpdateDiscountCommand(String tenantId, UUID discountId, Integer percentage, LocalDate validFrom,
                                     LocalDate validTo, Integer priority, Boolean stackable, BigDecimal cap,
                                     DiscountBase base) {
}
