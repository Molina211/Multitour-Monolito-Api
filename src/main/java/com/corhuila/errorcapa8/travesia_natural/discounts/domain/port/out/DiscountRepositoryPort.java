package com.corhuila.errorcapa8.travesia_natural.discounts.domain.port.out;

import com.corhuila.errorcapa8.travesia_natural.discounts.domain.model.Discount;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DiscountRepositoryPort {

    Discount save(Discount discount);

    Optional<Discount> findByTenantIdAndDiscountId(String tenantId, UUID discountId);

    List<Discount> findAllByTenantId(String tenantId);
}
