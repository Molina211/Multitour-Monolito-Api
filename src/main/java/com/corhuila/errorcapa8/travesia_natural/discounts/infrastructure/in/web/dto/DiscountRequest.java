package com.corhuila.errorcapa8.travesia_natural.discounts.infrastructure.in.web.dto;

import com.corhuila.errorcapa8.travesia_natural.discounts.domain.model.DiscountBase;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record DiscountRequest(UUID catalogItemId, int percentage, LocalDate validFrom, LocalDate validTo,
                               int priority, boolean stackable, BigDecimal cap, String base) {

    public DiscountBase toDiscountBase() {
        return switch (base) {
            case "original" -> DiscountBase.ORIGINAL_VALUE;
            case "subtotal" -> DiscountBase.PREVIOUS_SUBTOTAL;
            default -> throw new IllegalArgumentException("base must be 'original' or 'subtotal'");
        };
    }
}
