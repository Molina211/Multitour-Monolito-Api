package com.corhuila.errorcapa8.travesia_natural.tenants.application;

import com.corhuila.errorcapa8.travesia_natural.common.audit.AuditRecord;
import com.corhuila.errorcapa8.travesia_natural.common.audit.AuditRecorder;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.InvalidTenantException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.Tenant;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.in.DeactivateTenantCommand;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.in.DeactivateTenantUseCase;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.out.TenantRepositoryPort;
import org.springframework.stereotype.Service;

@Service
public class DeactivateTenantService implements DeactivateTenantUseCase {

    private final TenantRepositoryPort tenantRepositoryPort;
    private final AuditRecorder auditRecorder;

    public DeactivateTenantService(TenantRepositoryPort tenantRepositoryPort, AuditRecorder auditRecorder) {
        this.tenantRepositoryPort = tenantRepositoryPort;
        this.auditRecorder = auditRecorder;
    }

    @Override
    public Tenant deactivateTenant(DeactivateTenantCommand command) {
        if (command.reason() == null || command.reason().isBlank()) {
            throw new InvalidTenantException("reason is required to deactivate a tenant");
        }

        Tenant tenant = tenantRepositoryPort.findById(command.tenantId())
                .orElseThrow(() -> new TenantNotFoundException(command.tenantId()));

        Tenant deactivated = tenant.deactivate();
        tenantRepositoryPort.save(deactivated);

        auditRecorder.record(AuditRecord.of(
                deactivated.tenantId(), command.actorId(), "TENANT_DEACTIVATED", deactivated.tenantId(),
                command.reason(), "Activo", "Inactivo", "tenants", "Cambio de estado de operador"));

        return deactivated;
    }
}
