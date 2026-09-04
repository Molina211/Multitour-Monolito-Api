package com.corhuila.errorcapa8.travesia_natural.operations.domain.port.out;

import com.corhuila.errorcapa8.travesia_natural.operations.domain.model.Execution;

import java.util.Optional;
import java.util.UUID;

public interface ExecutionRepositoryPort {

    Execution save(Execution execution);

    Optional<Execution> findByTenantIdAndReservationId(String tenantId, UUID reservationId);
}
