package com.corhuila.errorcapa8.travesia_natural.discounts.infrastructure.in.web.dto;

import com.corhuila.errorcapa8.travesia_natural.discounts.domain.model.DiscountBase;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DiscountPatchRequest(Integer percentage, LocalDate validFrom, LocalDate validTo, Integer priority,
                                    Boolean stackable, BigDecimal cap, String base) {

    public DiscountBase toDiscountBase() {
        if (base == null) {
            return null;
        }
        return switch (base) {
            case "original" -> DiscountBase.ORIGINAL_VALUE;
            case "subtotal" -> DiscountBase.PREVIOUS_SUBTOTAL;
            default -> throw new IllegalArgumentException("base must be 'original' or 'subtotal'");
        };
    }
}
