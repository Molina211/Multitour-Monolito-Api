package com.corhuila.errorcapa8.travesia_natural.establishments.application;

import com.corhuila.errorcapa8.travesia_natural.establishments.domain.exception.EstablishmentNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.establishments.domain.model.AssociatedEstablishment;
import com.corhuila.errorcapa8.travesia_natural.establishments.domain.port.in.ReactivateEstablishmentUseCase;
import com.corhuila.errorcapa8.travesia_natural.establishments.domain.port.out.EstablishmentRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ReactivateEstablishmentService implements ReactivateEstablishmentUseCase {

    private final EstablishmentRepositoryPort establishmentRepositoryPort;

    public ReactivateEstablishmentService(EstablishmentRepositoryPort establishmentRepositoryPort) {
        this.establishmentRepositoryPort = establishmentRepositoryPort;
    }

    @Override
    public AssociatedEstablishment reactivateEstablishment(String tenantId, UUID establishmentId) {
        AssociatedEstablishment establishment = establishmentRepositoryPort
                .findByTenantIdAndEstablishmentId(tenantId, establishmentId)
                .orElseThrow(() -> new EstablishmentNotFoundException(establishmentId.toString()));

        return establishmentRepositoryPort.save(establishment.reactivate());
    }
}
