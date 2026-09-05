package com.corhuila.errorcapa8.travesia_natural.establishments.domain.port.in;

import com.corhuila.errorcapa8.travesia_natural.establishments.domain.model.AssociatedEstablishment;

import java.util.UUID;

public interface DeactivateEstablishmentUseCase {

    AssociatedEstablishment deactivateEstablishment(String tenantId, UUID establishmentId);
}
