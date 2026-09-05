package com.corhuila.errorcapa8.travesia_natural.establishments.infrastructure.out.persistence;

import com.corhuila.errorcapa8.travesia_natural.establishments.domain.model.AssociatedEstablishment;
import com.corhuila.errorcapa8.travesia_natural.establishments.domain.model.EstablishmentKind;
import com.corhuila.errorcapa8.travesia_natural.establishments.domain.port.out.EstablishmentRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class EstablishmentRepositoryAdapter implements EstablishmentRepositoryPort {

    private final EstablishmentJpaRepository jpaRepository;

    public EstablishmentRepositoryAdapter(EstablishmentJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public AssociatedEstablishment save(AssociatedEstablishment establishment) {
        jpaRepository.save(new EstablishmentEntity(
                establishment.establishmentId(),
                establishment.tenantId(),
                establishment.kind().name(),
                establishment.name(),
                establishment.description(),
                establishment.image(),
                establishment.active(),
                establishment.createdAt()));

        return establishment;
    }

    @Override
    public Optional<AssociatedEstablishment> findByTenantIdAndEstablishmentId(String tenantId,
                                                                               UUID establishmentId) {
        return jpaRepository.findByTenantIdAndEstablishmentId(tenantId, establishmentId)
                .map(EstablishmentRepositoryAdapter::toDomain);
    }

    @Override
    public List<AssociatedEstablishment> findAllByTenantId(String tenantId) {
        return jpaRepository.findAllByTenantId(tenantId).stream()
                .map(EstablishmentRepositoryAdapter::toDomain)
                .toList();
    }

    private static AssociatedEstablishment toDomain(EstablishmentEntity entity) {
        return AssociatedEstablishment.reconstitute(
                entity.getEstablishmentId(),
                entity.getTenantId(),
                EstablishmentKind.valueOf(entity.getKind()),
                entity.getName(),
                entity.getDescription(),
                entity.getImage(),
                entity.isActive(),
                entity.getCreatedAt());
    }
}
