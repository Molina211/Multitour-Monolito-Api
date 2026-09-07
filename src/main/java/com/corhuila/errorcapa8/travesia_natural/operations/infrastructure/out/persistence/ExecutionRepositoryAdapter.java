package com.corhuila.errorcapa8.travesia_natural.operations.infrastructure.out.persistence;

import com.corhuila.errorcapa8.travesia_natural.operations.domain.model.Execution;
import com.corhuila.errorcapa8.travesia_natural.operations.domain.port.out.ExecutionRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class ExecutionRepositoryAdapter implements ExecutionRepositoryPort {

    private final ExecutionJpaRepository jpaRepository;

    public ExecutionRepositoryAdapter(ExecutionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Execution save(Execution execution) {
        jpaRepository.save(new ExecutionEntity(
                execution.executionId(),
                execution.tenantId(),
                execution.reservationId(),
                execution.served(),
                execution.executed(),
                execution.causal(),
                execution.actorId(),
                execution.recordedAt()));

        return execution;
    }

    @Override
    public Optional<Execution> findByTenantIdAndReservationId(String tenantId, UUID reservationId) {
        return jpaRepository.findByTenantIdAndReservationId(tenantId, reservationId)
                .map(ExecutionRepositoryAdapter::toDomain);
    }

    private static Execution toDomain(ExecutionEntity entity) {
        return Execution.reconstitute(
                entity.getExecutionId(),
                entity.getTenantId(),
                entity.getReservationId(),
                entity.isServed(),
                entity.getExecuted(),
                entity.getCausal(),
                entity.getActorId(),
                entity.getRecordedAt());
    }
}
