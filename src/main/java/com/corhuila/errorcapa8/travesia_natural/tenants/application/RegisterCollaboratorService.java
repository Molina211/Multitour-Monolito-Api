package com.corhuila.errorcapa8.travesia_natural.tenants.application;

import com.corhuila.errorcapa8.travesia_natural.common.audit.AuditRecord;
import com.corhuila.errorcapa8.travesia_natural.common.audit.AuditRecorder;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.EmailAlreadyRegisteredException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantInactiveException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.Membership;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.PasswordPolicy;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.Tenant;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.TenantStatus;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.in.RegisterCollaboratorCommand;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.in.RegisterCollaboratorUseCase;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.out.MembershipRepositoryPort;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.out.TenantRepositoryPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class RegisterCollaboratorService implements RegisterCollaboratorUseCase {

    private final TenantRepositoryPort tenantRepositoryPort;
    private final MembershipRepositoryPort membershipRepositoryPort;
    private final PasswordEncoder passwordEncoder;
    private final AuditRecorder auditRecorder;

    public RegisterCollaboratorService(TenantRepositoryPort tenantRepositoryPort,
                                        MembershipRepositoryPort membershipRepositoryPort,
                                        PasswordEncoder passwordEncoder,
                                        AuditRecorder auditRecorder) {
        this.tenantRepositoryPort = tenantRepositoryPort;
        this.membershipRepositoryPort = membershipRepositoryPort;
        this.passwordEncoder = passwordEncoder;
        this.auditRecorder = auditRecorder;
    }

    @Override
    public Membership registerCollaborator(RegisterCollaboratorCommand command) {
        Tenant tenant = tenantRepositoryPort.findById(command.tenantId())
                .orElseThrow(() -> new TenantNotFoundException(command.tenantId()));

        if (tenant.tenantStatus() == TenantStatus.INACTIVO) {
            throw new TenantInactiveException(tenant.tenantId());
        }

        PasswordPolicy.validate(command.password());

        if (membershipRepositoryPort.existsByTenantIdAndEmail(tenant.tenantId(), command.email())) {
            throw new EmailAlreadyRegisteredException(tenant.tenantId(), command.email());
        }

        String passwordHash = passwordEncoder.encode(command.password());
        Membership collaborator = Membership.createOperationalCollaborator(
                tenant.tenantId(), command.name(), command.email(), passwordHash);

        membershipRepositoryPort.save(collaborator);

        auditRecorder.record(AuditRecord.of(
                tenant.tenantId(), command.actorId(), "COLLABORATOR_REGISTERED",
                collaborator.membershipId().toString(), null));

        return collaborator;
    }
}
