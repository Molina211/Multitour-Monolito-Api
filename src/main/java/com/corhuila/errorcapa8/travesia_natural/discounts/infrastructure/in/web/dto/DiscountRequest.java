package com.corhuila.errorcapa8.travesia_natural.discounts.infrastructure.in.web.dto;

import com.corhuila.errorcapa8.travesia_natural.discounts.domain.model.DiscountBase;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * {@code priority}/{@code stackable} son {@link Integer}/{@link Boolean} (no
 * primitivos) porque la migración les da {@code DEFAULT 0}/{@code DEFAULT FALSE}: el
 * cliente puede omitirlos y el controlador resuelve el valor por defecto, en vez de que
 * Jackson falle al mapear {@code null} sobre un primitivo.
 */
public record DiscountRequest(UUID catalogItemId, int percentage, LocalDate validFrom, LocalDate validTo,
                               Integer priority, Boolean stackable, BigDecimal cap, String base) {

    public int priorityOrDefault() {
        return priority != null ? priority : 0;
    }

    public boolean stackableOrDefault() {
        return stackable != null && stackable;
    }

    public DiscountBase toDiscountBase() {
        return switch (base) {
            case "original" -> DiscountBase.ORIGINAL_VALUE;
            case "subtotal" -> DiscountBase.PREVIOUS_SUBTOTAL;
            default -> throw new IllegalArgumentException("base must be 'original' or 'subtotal'");
        };
    }
}
