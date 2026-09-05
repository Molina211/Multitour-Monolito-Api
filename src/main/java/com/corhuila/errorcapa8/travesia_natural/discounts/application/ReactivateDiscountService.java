package com.corhuila.errorcapa8.travesia_natural.discounts.application;

import com.corhuila.errorcapa8.travesia_natural.discounts.domain.exception.DiscountNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.discounts.domain.model.Discount;
import com.corhuila.errorcapa8.travesia_natural.discounts.domain.port.in.ReactivateDiscountUseCase;
import com.corhuila.errorcapa8.travesia_natural.discounts.domain.port.out.DiscountRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ReactivateDiscountService implements ReactivateDiscountUseCase {

    private final DiscountRepositoryPort discountRepositoryPort;

    public ReactivateDiscountService(DiscountRepositoryPort discountRepositoryPort) {
        this.discountRepositoryPort = discountRepositoryPort;
    }

    @Override
    public Discount reactivateDiscount(String tenantId, UUID discountId) {
        Discount discount = discountRepositoryPort.findByTenantIdAndDiscountId(tenantId, discountId)
                .orElseThrow(() -> new DiscountNotFoundException(discountId.toString()));

        return discountRepositoryPort.save(discount.reactivate());
    }
}
