package com.corhuila.errorcapa8.travesia_natural.discounts.domain.model;

/**
 * Value over which the discount percentage is calculated (spec 008, RF-005A/RF-005B):
 * either the catalog item's original price, or the subtotal left by a previous discount
 * already applied to the same reservation line. No calculation engine consumes this yet
 * (spec 008 only persists the parameter) — see spec.md "Fuera de alcance".
 */
public enum DiscountBase {
    ORIGINAL_VALUE,
    PREVIOUS_SUBTOTAL
}
