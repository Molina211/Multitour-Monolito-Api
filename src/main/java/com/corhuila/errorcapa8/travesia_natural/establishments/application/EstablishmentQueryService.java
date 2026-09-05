package com.corhuila.errorcapa8.travesia_natural.establishments.application;

import com.corhuila.errorcapa8.travesia_natural.establishments.domain.model.AssociatedEstablishment;
import com.corhuila.errorcapa8.travesia_natural.establishments.domain.port.in.EstablishmentQueryUseCase;
import com.corhuila.errorcapa8.travesia_natural.establishments.domain.port.out.EstablishmentRepositoryPort;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.out.TenantRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EstablishmentQueryService implements EstablishmentQueryUseCase {

    private final TenantRepositoryPort tenantRepositoryPort;
    private final EstablishmentRepositoryPort establishmentRepositoryPort;

    public EstablishmentQueryService(TenantRepositoryPort tenantRepositoryPort,
                                      EstablishmentRepositoryPort establishmentRepositoryPort) {
        this.tenantRepositoryPort = tenantRepositoryPort;
        this.establishmentRepositoryPort = establishmentRepositoryPort;
    }

    @Override
    public List<AssociatedEstablishment> listByTenant(String tenantId) {
        if (!tenantRepositoryPort.existsById(tenantId)) {
            throw new TenantNotFoundException(tenantId);
        }

        return establishmentRepositoryPort.findAllByTenantId(tenantId);
    }
}
