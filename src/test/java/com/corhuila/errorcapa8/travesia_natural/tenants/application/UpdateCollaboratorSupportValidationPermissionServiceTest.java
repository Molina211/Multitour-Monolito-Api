package com.corhuila.errorcapa8.travesia_natural.tenants.application;

import com.corhuila.errorcapa8.travesia_natural.common.audit.AuditRecorder;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantInactiveException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantPermissionNotAllowedException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.Membership;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.MembershipRole;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.MembershipStatus;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.Tenant;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.in.UpdateCollaboratorSupportValidationPermissionCommand;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.out.MembershipRepositoryPort;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.out.TenantRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateCollaboratorSupportValidationPermissionServiceTest {

    private static final String TENANT_ID = "travesia-natural";

    @Mock
    private TenantRepositoryPort tenantRepositoryPort;
    @Mock
    private MembershipRepositoryPort membershipRepositoryPort;
    @Mock
    private AuditRecorder auditRecorder;

    private UpdateCollaboratorSupportValidationPermissionService service;

    @BeforeEach
    void setUp() {
        service = new UpdateCollaboratorSupportValidationPermissionService(tenantRepositoryPort,
                membershipRepositoryPort, auditRecorder);
    }

    private Membership anAdministrator(UUID membershipId) {
        return Membership.reconstitute(membershipId, TENANT_ID, null, null, "admin@correo.com", null,
                "hashed-password", MembershipRole.ADMINISTRATOR, MembershipStatus.ACTIVA, Instant.now());
    }

    @Test
    void updatesPermissionWhenActorIsAdministrator() {
        UUID actorId = UUID.randomUUID();
        Tenant tenant = Tenant.create(TENANT_ID, "Travesia Natural");
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(tenant));
        when(membershipRepositoryPort.findByTenantIdAndMembershipId(TENANT_ID, actorId))
                .thenReturn(Optional.of(anAdministrator(actorId)));

        Tenant result = service.updateCollaboratorSupportValidationPermission(
                new UpdateCollaboratorSupportValidationPermissionCommand(TENANT_ID, actorId.toString(), true));

        assertThat(result.allowCollaboratorSupportValidation()).isTrue();
    }

    @Test
    void rejectsWhenTenantDoesNotExist() {
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateCollaboratorSupportValidationPermission(
                new UpdateCollaboratorSupportValidationPermissionCommand(TENANT_ID, UUID.randomUUID().toString(),
                        true)))
                .isInstanceOf(TenantNotFoundException.class);
    }

    @Test
    void rejectsWhenTenantIsInactive() {
        Tenant inactiveTenant = Tenant.create(TENANT_ID, "Travesia Natural").deactivate();
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(inactiveTenant));

        assertThatThrownBy(() -> service.updateCollaboratorSupportValidationPermission(
                new UpdateCollaboratorSupportValidationPermissionCommand(TENANT_ID, UUID.randomUUID().toString(),
                        true)))
                .isInstanceOf(TenantInactiveException.class);
    }

    @Test
    void rejectsWhenActorIdIsNotAValidUuid() {
        Tenant tenant = Tenant.create(TENANT_ID, "Travesia Natural");
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(tenant));

        assertThatThrownBy(() -> service.updateCollaboratorSupportValidationPermission(
                new UpdateCollaboratorSupportValidationPermissionCommand(TENANT_ID, "not-a-uuid", true)))
                .isInstanceOf(TenantPermissionNotAllowedException.class);
    }

    @Test
    void rejectsWhenActorMembershipDoesNotExist() {
        UUID actorId = UUID.randomUUID();
        Tenant tenant = Tenant.create(TENANT_ID, "Travesia Natural");
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(tenant));
        when(membershipRepositoryPort.findByTenantIdAndMembershipId(TENANT_ID, actorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateCollaboratorSupportValidationPermission(
                new UpdateCollaboratorSupportValidationPermissionCommand(TENANT_ID, actorId.toString(), true)))
                .isInstanceOf(TenantPermissionNotAllowedException.class);
    }

    @Test
    void rejectsWhenActorIsNotAnAdministrator() {
        UUID actorId = UUID.randomUUID();
        Tenant tenant = Tenant.create(TENANT_ID, "Travesia Natural");
        Membership collaborator = Membership.reconstitute(actorId, TENANT_ID, "Juan", null, "juan@correo.com", null,
                "hashed-password", MembershipRole.OPERATIONAL_COLLABORATOR, MembershipStatus.ACTIVA, Instant.now());
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(tenant));
        when(membershipRepositoryPort.findByTenantIdAndMembershipId(TENANT_ID, actorId))
                .thenReturn(Optional.of(collaborator));

        assertThatThrownBy(() -> service.updateCollaboratorSupportValidationPermission(
                new UpdateCollaboratorSupportValidationPermissionCommand(TENANT_ID, actorId.toString(), true)))
                .isInstanceOf(TenantPermissionNotAllowedException.class);
    }
}
