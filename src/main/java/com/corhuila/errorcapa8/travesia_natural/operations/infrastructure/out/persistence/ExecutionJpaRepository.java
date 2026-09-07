package com.corhuila.errorcapa8.travesia_natural.operations.infrastructure.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ExecutionJpaRepository extends JpaRepository<ExecutionEntity, UUID> {

    Optional<ExecutionEntity> findByTenantIdAndReservationId(String tenantId, UUID reservationId);
}
