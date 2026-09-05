package com.corhuila.errorcapa8.travesia_natural.tenants.application;

import com.corhuila.errorcapa8.travesia_natural.common.audit.AuditRecord;
import com.corhuila.errorcapa8.travesia_natural.common.audit.AuditRecorder;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.InvalidTenantException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.Tenant;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.in.ReactivateTenantCommand;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.in.ReactivateTenantUseCase;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.out.TenantRepositoryPort;
import org.springframework.stereotype.Service;

@Service
public class ReactivateTenantService implements ReactivateTenantUseCase {

    private final TenantRepositoryPort tenantRepositoryPort;
    private final AuditRecorder auditRecorder;

    public ReactivateTenantService(TenantRepositoryPort tenantRepositoryPort, AuditRecorder auditRecorder) {
        this.tenantRepositoryPort = tenantRepositoryPort;
        this.auditRecorder = auditRecorder;
    }

    @Override
    public Tenant reactivateTenant(ReactivateTenantCommand command) {
        if (command.reason() == null || command.reason().isBlank()) {
            throw new InvalidTenantException("reason is required to reactivate a tenant");
        }

        Tenant tenant = tenantRepositoryPort.findById(command.tenantId())
                .orElseThrow(() -> new TenantNotFoundException(command.tenantId()));

        Tenant reactivated = tenant.reactivate();
        tenantRepositoryPort.save(reactivated);

        auditRecorder.record(AuditRecord.of(
                reactivated.tenantId(), command.actorId(), "TENANT_REACTIVATED", reactivated.tenantId(),
                command.reason(), "Inactivo", "Activo", "tenants", "Cambio de estado de operador"));

        return reactivated;
    }
}
