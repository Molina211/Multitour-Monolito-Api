package com.corhuila.errorcapa8.travesia_natural.establishments.domain.port.out;

import com.corhuila.errorcapa8.travesia_natural.establishments.domain.model.AssociatedEstablishment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EstablishmentRepositoryPort {

    AssociatedEstablishment save(AssociatedEstablishment establishment);

    Optional<AssociatedEstablishment> findByTenantIdAndEstablishmentId(String tenantId, UUID establishmentId);

    List<AssociatedEstablishment> findAllByTenantId(String tenantId);
}
