package com.corhuila.errorcapa8.travesia_natural.operations.infrastructure.out.persistence;

import com.corhuila.errorcapa8.travesia_natural.operations.domain.model.OperationCost;
import com.corhuila.errorcapa8.travesia_natural.operations.domain.port.out.OperationCostRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class OperationCostRepositoryAdapter implements OperationCostRepositoryPort {

    private final OperationCostJpaRepository jpaRepository;

    public OperationCostRepositoryAdapter(OperationCostJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public OperationCost save(OperationCost operationCost) {
        jpaRepository.save(new OperationCostEntity(
                operationCost.costId(),
                operationCost.tenantId(),
                operationCost.reservationId(),
                operationCost.concept(),
                operationCost.amount(),
                operationCost.actorId(),
                operationCost.recordedAt()));

        return operationCost;
    }

    @Override
    public List<OperationCost> findAllByTenantIdAndReservationIdOrderByRecordedAt(String tenantId,
                                                                                   UUID reservationId) {
        return jpaRepository.findAllByTenantIdAndReservationIdOrderByRecordedAtAsc(tenantId, reservationId).stream()
                .map(OperationCostRepositoryAdapter::toDomain)
                .toList();
    }

    @Override
    public List<OperationCost> findAllByTenantId(String tenantId) {
        return jpaRepository.findAllByTenantId(tenantId).stream()
                .map(OperationCostRepositoryAdapter::toDomain)
                .toList();
    }

    private static OperationCost toDomain(OperationCostEntity entity) {
        return OperationCost.reconstitute(
                entity.getCostId(),
                entity.getTenantId(),
                entity.getReservationId(),
                entity.getConcept(),
                entity.getAmount(),
                entity.getActorId(),
                entity.getRecordedAt());
    }
}
