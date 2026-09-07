package com.corhuila.errorcapa8.travesia_natural.operations.application;

import com.corhuila.errorcapa8.travesia_natural.operations.domain.exception.ExecutionNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.operations.domain.model.Execution;
import com.corhuila.errorcapa8.travesia_natural.operations.domain.port.in.ExecutionQueryUseCase;
import com.corhuila.errorcapa8.travesia_natural.operations.domain.port.out.ExecutionRepositoryPort;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantInactiveException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.Tenant;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.TenantStatus;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.out.TenantRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ExecutionQueryService implements ExecutionQueryUseCase {

    private final TenantRepositoryPort tenantRepositoryPort;
    private final ExecutionRepositoryPort executionRepositoryPort;

    public ExecutionQueryService(TenantRepositoryPort tenantRepositoryPort,
                                  ExecutionRepositoryPort executionRepositoryPort) {
        this.tenantRepositoryPort = tenantRepositoryPort;
        this.executionRepositoryPort = executionRepositoryPort;
    }

    @Override
    public Execution getByReservation(String tenantId, UUID reservationId) {
        requireActiveTenant(tenantId);

        return executionRepositoryPort.findByTenantIdAndReservationId(tenantId, reservationId)
                .orElseThrow(() -> new ExecutionNotFoundException(reservationId.toString()));
    }

    private void requireActiveTenant(String tenantId) {
        Tenant tenant = tenantRepositoryPort.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));

        if (tenant.tenantStatus() == TenantStatus.INACTIVO) {
            throw new TenantInactiveException(tenant.tenantId());
        }
    }
}
