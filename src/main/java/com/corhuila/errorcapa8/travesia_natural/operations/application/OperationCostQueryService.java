package com.corhuila.errorcapa8.travesia_natural.operations.application;

import com.corhuila.errorcapa8.travesia_natural.operations.domain.model.OperationCost;
import com.corhuila.errorcapa8.travesia_natural.operations.domain.port.in.OperationCostQueryUseCase;
import com.corhuila.errorcapa8.travesia_natural.operations.domain.port.out.OperationCostRepositoryPort;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.exception.ReservationNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.out.ReservationRepositoryPort;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantInactiveException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.Tenant;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.TenantStatus;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.out.TenantRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class OperationCostQueryService implements OperationCostQueryUseCase {

    private final TenantRepositoryPort tenantRepositoryPort;
    private final ReservationRepositoryPort reservationRepositoryPort;
    private final OperationCostRepositoryPort operationCostRepositoryPort;

    public OperationCostQueryService(TenantRepositoryPort tenantRepositoryPort,
                                      ReservationRepositoryPort reservationRepositoryPort,
                                      OperationCostRepositoryPort operationCostRepositoryPort) {
        this.tenantRepositoryPort = tenantRepositoryPort;
        this.reservationRepositoryPort = reservationRepositoryPort;
        this.operationCostRepositoryPort = operationCostRepositoryPort;
    }

    @Override
    public List<OperationCost> listByReservation(String tenantId, UUID reservationId) {
        requireActiveTenant(tenantId);

        reservationRepositoryPort.findByTenantIdAndReservationId(tenantId, reservationId)
                .orElseThrow(() -> new ReservationNotFoundException(reservationId.toString()));

        return operationCostRepositoryPort.findAllByTenantIdAndReservationIdOrderByRecordedAt(tenantId,
                reservationId);
    }

    private void requireActiveTenant(String tenantId) {
        Tenant tenant = tenantRepositoryPort.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));

        if (tenant.tenantStatus() == TenantStatus.INACTIVO) {
            throw new TenantInactiveException(tenant.tenantId());
        }
    }
}
