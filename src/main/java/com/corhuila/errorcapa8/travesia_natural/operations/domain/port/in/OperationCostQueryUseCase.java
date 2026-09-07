package com.corhuila.errorcapa8.travesia_natural.operations.domain.port.in;

import com.corhuila.errorcapa8.travesia_natural.operations.domain.model.OperationCost;

import java.util.List;
import java.util.UUID;

public interface OperationCostQueryUseCase {

    List<OperationCost> listByReservation(String tenantId, UUID reservationId);
}
