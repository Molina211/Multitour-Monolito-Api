package com.corhuila.errorcapa8.travesia_natural.discounts.domain.port.in;

import com.corhuila.errorcapa8.travesia_natural.discounts.domain.model.Discount;

import java.util.UUID;

public interface ReactivateDiscountUseCase {

    Discount reactivateDiscount(String tenantId, UUID discountId);
}
