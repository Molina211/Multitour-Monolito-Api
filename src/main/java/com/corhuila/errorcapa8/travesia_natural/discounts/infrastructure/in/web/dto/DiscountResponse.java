package com.corhuila.errorcapa8.travesia_natural.discounts.infrastructure.in.web.dto;

import com.corhuila.errorcapa8.travesia_natural.discounts.domain.model.Discount;
import com.corhuila.errorcapa8.travesia_natural.discounts.domain.model.DiscountBase;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record DiscountResponse(UUID discountId, String tenantId, UUID catalogItemId, int percentage,
                                LocalDate validFrom, LocalDate validTo, int priority, boolean stackable,
                                BigDecimal cap, String base, boolean active, Instant createdAt) {

    public static DiscountResponse from(Discount discount) {
        return new DiscountResponse(
                discount.discountId(),
                discount.tenantId(),
                discount.catalogItemId(),
                discount.percentage(),
                discount.validFrom(),
                discount.validTo(),
                discount.priority(),
                discount.stackable(),
                discount.cap(),
                toLabel(discount.base()),
                discount.active(),
                discount.createdAt());
    }

    private static String toLabel(DiscountBase base) {
        return switch (base) {
            case ORIGINAL_VALUE -> "original";
            case PREVIOUS_SUBTOTAL -> "subtotal";
        };
    }
}
