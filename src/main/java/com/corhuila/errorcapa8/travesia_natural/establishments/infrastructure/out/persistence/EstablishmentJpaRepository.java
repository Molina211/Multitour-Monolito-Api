package com.corhuila.errorcapa8.travesia_natural.establishments.infrastructure.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EstablishmentJpaRepository extends JpaRepository<EstablishmentEntity, UUID> {

    Optional<EstablishmentEntity> findByTenantIdAndEstablishmentId(String tenantId, UUID establishmentId);

    List<EstablishmentEntity> findAllByTenantId(String tenantId);
}
