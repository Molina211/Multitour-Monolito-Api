package com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.in;

import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.Membership;

public interface RegisterCustomerUseCase {

    Membership registerCustomer(RegisterCustomerCommand command);
}
