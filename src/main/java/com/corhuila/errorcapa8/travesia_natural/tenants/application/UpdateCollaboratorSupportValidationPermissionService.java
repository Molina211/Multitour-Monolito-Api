package com.corhuila.errorcapa8.travesia_natural.tenants.application;

import com.corhuila.errorcapa8.travesia_natural.common.audit.AuditRecord;
import com.corhuila.errorcapa8.travesia_natural.common.audit.AuditRecorder;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantInactiveException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantPermissionNotAllowedException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.Membership;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.MembershipRole;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.Tenant;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.TenantStatus;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.in.UpdateCollaboratorSupportValidationPermissionCommand;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.in.UpdateCollaboratorSupportValidationPermissionUseCase;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.out.MembershipRepositoryPort;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.out.TenantRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UpdateCollaboratorSupportValidationPermissionService
        implements UpdateCollaboratorSupportValidationPermissionUseCase {

    private final TenantRepositoryPort tenantRepositoryPort;
    private final MembershipRepositoryPort membershipRepositoryPort;
    private final AuditRecorder auditRecorder;

    public UpdateCollaboratorSupportValidationPermissionService(TenantRepositoryPort tenantRepositoryPort,
                                                                  MembershipRepositoryPort membershipRepositoryPort,
                                                                  AuditRecorder auditRecorder) {
        this.tenantRepositoryPort = tenantRepositoryPort;
        this.membershipRepositoryPort = membershipRepositoryPort;
        this.auditRecorder = auditRecorder;
    }

    @Override
    public Tenant updateCollaboratorSupportValidationPermission(
            UpdateCollaboratorSupportValidationPermissionCommand command) {
        Tenant tenant = tenantRepositoryPort.findById(command.tenantId())
                .orElseThrow(() -> new TenantNotFoundException(command.tenantId()));

        if (tenant.tenantStatus() == TenantStatus.INACTIVO) {
            throw new TenantInactiveException(tenant.tenantId());
        }

        requireAdministratorActor(command.tenantId(), command.actorId());

        Tenant updated = tenant.updateCollaboratorSupportValidationPermission(command.allow());
        tenantRepositoryPort.save(updated);

        auditRecorder.record(AuditRecord.of(
                updated.tenantId(), command.actorId(), "COLLABORATOR_SUPPORT_VALIDATION_PERMISSION_UPDATED",
                updated.tenantId(), "allow=" + command.allow()));

        return updated;
    }

    private void requireAdministratorActor(String tenantId, String actorId) {
        if (actorId == null) {
            throw new TenantPermissionNotAllowedException("actorId must be a valid membershipId: null");
        }

        UUID actorMembershipId;
        try {
            actorMembershipId = UUID.fromString(actorId);
        } catch (IllegalArgumentException e) {
            throw new TenantPermissionNotAllowedException("actorId must be a valid membershipId: " + actorId);
        }

        Membership actor = membershipRepositoryPort.findByTenantIdAndMembershipId(tenantId, actorMembershipId)
                .orElseThrow(() -> new TenantPermissionNotAllowedException(
                        "membership not found for actorId: " + actorId + " in tenant " + tenantId));

        if (actor.role() != MembershipRole.ADMINISTRATOR) {
            throw new TenantPermissionNotAllowedException(
                    "only an ADMINISTRATOR can change the collaborator support validation permission, actor role: "
                            + actor.role());
        }
    }
}
