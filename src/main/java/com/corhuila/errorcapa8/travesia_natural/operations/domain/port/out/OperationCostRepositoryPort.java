package com.corhuila.errorcapa8.travesia_natural.operations.domain.port.out;

import com.corhuila.errorcapa8.travesia_natural.operations.domain.model.OperationCost;

import java.util.List;
import java.util.UUID;

public interface OperationCostRepositoryPort {

    OperationCost save(OperationCost operationCost);

    List<OperationCost> findAllByTenantIdAndReservationIdOrderByRecordedAt(String tenantId, UUID reservationId);
}
