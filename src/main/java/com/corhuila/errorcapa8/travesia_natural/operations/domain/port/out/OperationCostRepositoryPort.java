package com.corhuila.errorcapa8.travesia_natural.operations.domain.port.out;

import com.corhuila.errorcapa8.travesia_natural.operations.domain.model.OperationCost;

import java.util.List;
import java.util.UUID;

public interface OperationCostRepositoryPort {

    OperationCost save(OperationCost operationCost);

    List<OperationCost> findAllByTenantIdAndReservationIdOrderByRecordedAt(String tenantId, UUID reservationId);

    /**
     * Todos los costos operacionales de un tenant, sin filtrar por reserva — la
     * consolidación mensual (RF-012, spec 013) filtra por {@code recordedAt} en la capa
     * de aplicación, igual que {@code findAllByTenantId} en {@code reservations}.
     */
    List<OperationCost> findAllByTenantId(String tenantId);
}
