package com.corhuila.errorcapa8.travesia_natural.discounts.domain.port.in;

import com.corhuila.errorcapa8.travesia_natural.discounts.domain.model.Discount;

public interface UpdateDiscountUseCase {

    Discount updateDiscount(UpdateDiscountCommand command);
}
