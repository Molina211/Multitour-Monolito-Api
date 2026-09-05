package com.corhuila.errorcapa8.travesia_natural.establishments.domain.port.in;

import com.corhuila.errorcapa8.travesia_natural.establishments.domain.model.AssociatedEstablishment;

import java.util.UUID;

public interface ReactivateEstablishmentUseCase {

    AssociatedEstablishment reactivateEstablishment(String tenantId, UUID establishmentId);
}
