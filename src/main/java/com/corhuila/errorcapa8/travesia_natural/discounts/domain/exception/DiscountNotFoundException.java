package com.corhuila.errorcapa8.travesia_natural.discounts.domain.exception;

public class DiscountNotFoundException extends RuntimeException {

    public DiscountNotFoundException(String discountId) {
        super("discount not found: " + discountId);
    }
}
