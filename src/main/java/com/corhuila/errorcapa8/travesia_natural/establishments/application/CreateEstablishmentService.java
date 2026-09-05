package com.corhuila.errorcapa8.travesia_natural.establishments.application;

import com.corhuila.errorcapa8.travesia_natural.establishments.domain.model.AssociatedEstablishment;
import com.corhuila.errorcapa8.travesia_natural.establishments.domain.port.in.CreateEstablishmentCommand;
import com.corhuila.errorcapa8.travesia_natural.establishments.domain.port.in.CreateEstablishmentUseCase;
import com.corhuila.errorcapa8.travesia_natural.establishments.domain.port.out.EstablishmentRepositoryPort;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantInactiveException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.Tenant;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.TenantStatus;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.out.TenantRepositoryPort;
import org.springframework.stereotype.Service;

@Service
public class CreateEstablishmentService implements CreateEstablishmentUseCase {

    private final TenantRepositoryPort tenantRepositoryPort;
    private final EstablishmentRepositoryPort establishmentRepositoryPort;

    public CreateEstablishmentService(TenantRepositoryPort tenantRepositoryPort,
                                       EstablishmentRepositoryPort establishmentRepositoryPort) {
        this.tenantRepositoryPort = tenantRepositoryPort;
        this.establishmentRepositoryPort = establishmentRepositoryPort;
    }

    @Override
    public AssociatedEstablishment createEstablishment(CreateEstablishmentCommand command) {
        Tenant tenant = tenantRepositoryPort.findById(command.tenantId())
                .orElseThrow(() -> new TenantNotFoundException(command.tenantId()));

        if (tenant.tenantStatus() == TenantStatus.INACTIVO) {
            throw new TenantInactiveException(tenant.tenantId());
        }

        AssociatedEstablishment establishment = AssociatedEstablishment.create(
                tenant.tenantId(), command.kind(), command.name(), command.description(), command.image());

        return establishmentRepositoryPort.save(establishment);
    }
}
