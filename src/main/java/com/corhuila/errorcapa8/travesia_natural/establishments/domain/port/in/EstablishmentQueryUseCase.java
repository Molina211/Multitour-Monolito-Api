package com.corhuila.errorcapa8.travesia_natural.establishments.domain.port.in;

import com.corhuila.errorcapa8.travesia_natural.establishments.domain.model.AssociatedEstablishment;

import java.util.List;

public interface EstablishmentQueryUseCase {

    List<AssociatedEstablishment> listByTenant(String tenantId);
}
