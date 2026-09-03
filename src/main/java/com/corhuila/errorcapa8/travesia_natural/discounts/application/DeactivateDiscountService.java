package com.corhuila.errorcapa8.travesia_natural.discounts.application;

import com.corhuila.errorcapa8.travesia_natural.discounts.domain.exception.DiscountNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.discounts.domain.model.Discount;
import com.corhuila.errorcapa8.travesia_natural.discounts.domain.port.in.DeactivateDiscountUseCase;
import com.corhuila.errorcapa8.travesia_natural.discounts.domain.port.out.DiscountRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class DeactivateDiscountService implements DeactivateDiscountUseCase {

    private final DiscountRepositoryPort discountRepositoryPort;

    public DeactivateDiscountService(DiscountRepositoryPort discountRepositoryPort) {
        this.discountRepositoryPort = discountRepositoryPort;
    }

    @Override
    public Discount deactivateDiscount(String tenantId, UUID discountId) {
        Discount discount = discountRepositoryPort.findByTenantIdAndDiscountId(tenantId, discountId)
                .orElseThrow(() -> new DiscountNotFoundException(discountId.toString()));

        return discountRepositoryPort.save(discount.deactivate());
    }
}
