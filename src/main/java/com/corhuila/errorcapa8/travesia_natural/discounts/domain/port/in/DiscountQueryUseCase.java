package com.corhuila.errorcapa8.travesia_natural.discounts.domain.port.in;

import com.corhuila.errorcapa8.travesia_natural.discounts.domain.model.Discount;

import java.util.List;
import java.util.UUID;

public interface DiscountQueryUseCase {

    Discount getById(String tenantId, UUID discountId);

    List<Discount> listByTenant(String tenantId);
}
