package com.corhuila.errorcapa8.travesia_natural.operations.infrastructure.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OperationCostJpaRepository extends JpaRepository<OperationCostEntity, UUID> {

    List<OperationCostEntity> findAllByTenantIdAndReservationIdOrderByRecordedAtAsc(String tenantId,
                                                                                     UUID reservationId);
}
