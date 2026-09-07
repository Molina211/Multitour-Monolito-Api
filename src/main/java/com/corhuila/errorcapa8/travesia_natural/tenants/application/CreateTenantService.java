package com.corhuila.errorcapa8.travesia_natural.tenants.application;

import com.corhuila.errorcapa8.travesia_natural.common.audit.AuditRecord;
import com.corhuila.errorcapa8.travesia_natural.common.audit.AuditRecorder;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantAlreadyExistsException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.Membership;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.Tenant;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.in.CreateTenantCommand;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.in.CreateTenantUseCase;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.out.MembershipRepositoryPort;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.out.TenantRepositoryPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class CreateTenantService implements CreateTenantUseCase {

    private final TenantRepositoryPort tenantRepositoryPort;
    private final MembershipRepositoryPort membershipRepositoryPort;
    private final PasswordEncoder passwordEncoder;
    private final AuditRecorder auditRecorder;

    public CreateTenantService(TenantRepositoryPort tenantRepositoryPort,
                                MembershipRepositoryPort membershipRepositoryPort,
                                PasswordEncoder passwordEncoder,
                                AuditRecorder auditRecorder) {
        this.tenantRepositoryPort = tenantRepositoryPort;
        this.membershipRepositoryPort = membershipRepositoryPort;
        this.passwordEncoder = passwordEncoder;
        this.auditRecorder = auditRecorder;
    }

    @Override
    public Tenant createTenant(CreateTenantCommand command) {
        if (tenantRepositoryPort.existsById(command.tenantId())) {
            throw new TenantAlreadyExistsException(command.tenantId());
        }

        Tenant tenant = Tenant.create(command.tenantId(), command.commercialName());
        tenantRepositoryPort.save(tenant);

        String passwordHash = passwordEncoder.encode(command.administratorPassword());
        Membership administrator = Membership.createAdministrator(
                tenant.tenantId(), command.administratorEmail(), passwordHash);
        membershipRepositoryPort.save(administrator);

        auditRecorder.record(AuditRecord.of(
                tenant.tenantId(), command.actorId(), "TENANT_CREATED", tenant.tenantId(), null));

        return tenant;
    }
}
