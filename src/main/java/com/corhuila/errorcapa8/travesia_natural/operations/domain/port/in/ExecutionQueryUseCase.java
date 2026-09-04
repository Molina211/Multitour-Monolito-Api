package com.corhuila.errorcapa8.travesia_natural.operations.domain.port.in;

import com.corhuila.errorcapa8.travesia_natural.operations.domain.model.Execution;

import java.util.UUID;

public interface ExecutionQueryUseCase {

    Execution getByReservation(String tenantId, UUID reservationId);
}
