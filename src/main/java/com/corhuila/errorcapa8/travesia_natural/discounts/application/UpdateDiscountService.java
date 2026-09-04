package com.corhuila.errorcapa8.travesia_natural.discounts.application;

import com.corhuila.errorcapa8.travesia_natural.discounts.domain.exception.DiscountNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.discounts.domain.model.Discount;
import com.corhuila.errorcapa8.travesia_natural.discounts.domain.port.in.UpdateDiscountCommand;
import com.corhuila.errorcapa8.travesia_natural.discounts.domain.port.in.UpdateDiscountUseCase;
import com.corhuila.errorcapa8.travesia_natural.discounts.domain.port.out.DiscountRepositoryPort;
import org.springframework.stereotype.Service;

@Service
public class UpdateDiscountService implements UpdateDiscountUseCase {

    private final DiscountRepositoryPort discountRepositoryPort;

    public UpdateDiscountService(DiscountRepositoryPort discountRepositoryPort) {
        this.discountRepositoryPort = discountRepositoryPort;
    }

    @Override
    public Discount updateDiscount(UpdateDiscountCommand command) {
        Discount discount = discountRepositoryPort
                .findByTenantIdAndDiscountId(command.tenantId(), command.discountId())
                .orElseThrow(() -> new DiscountNotFoundException(command.discountId().toString()));

        Discount updated = discount.update(
                command.percentage(), command.validFrom(), command.validTo(), command.priority(),
                command.stackable(), command.cap(), command.base());

        return discountRepositoryPort.save(updated);
    }
}
