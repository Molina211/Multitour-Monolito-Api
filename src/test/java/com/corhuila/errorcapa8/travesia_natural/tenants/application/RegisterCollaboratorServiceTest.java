package com.corhuila.errorcapa8.travesia_natural.tenants.application;

import com.corhuila.errorcapa8.travesia_natural.common.audit.AuditRecord;
import com.corhuila.errorcapa8.travesia_natural.common.audit.AuditRecorder;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.EmailAlreadyRegisteredException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.InvalidTenantException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantInactiveException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.Membership;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.Tenant;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.in.RegisterCollaboratorCommand;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.out.MembershipRepositoryPort;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.out.TenantRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterCollaboratorServiceTest {

    private static final String TENANT_ID = "travesia-natural";
    private static final String EMAIL = "colaborador@correo.com";
    private static final String VALID_PASSWORD = "Password1!";

    @Mock
    private TenantRepositoryPort tenantRepositoryPort;
    @Mock
    private MembershipRepositoryPort membershipRepositoryPort;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuditRecorder auditRecorder;

    private RegisterCollaboratorService registerCollaboratorService;

    @BeforeEach
    void setUp() {
        registerCollaboratorService = new RegisterCollaboratorService(tenantRepositoryPort, membershipRepositoryPort,
                passwordEncoder, auditRecorder);
    }

    private RegisterCollaboratorCommand aCommand(String password) {
        return new RegisterCollaboratorCommand(TENANT_ID, "Juan Perez", EMAIL, password, "actor-1");
    }

    @Test
    void registersACollaboratorSuccessfullyAndRecordsAudit() {
        Tenant tenant = Tenant.create(TENANT_ID, "Travesia Natural");
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(tenant));
        when(membershipRepositoryPort.existsByTenantIdAndEmail(TENANT_ID, EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(VALID_PASSWORD)).thenReturn("hashed-password");

        Membership result = registerCollaboratorService.registerCollaborator(aCommand(VALID_PASSWORD));

        assertThat(result.email()).isEqualTo(EMAIL);
        verify(membershipRepositoryPort).save(result);

        ArgumentCaptor<AuditRecord> captor = ArgumentCaptor.forClass(AuditRecord.class);
        verify(auditRecorder).record(captor.capture());
        assertThat(captor.getValue().action()).isEqualTo("COLLABORATOR_REGISTERED");
    }

    @Test
    void rejectsWhenTenantDoesNotExist() {
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> registerCollaboratorService.registerCollaborator(aCommand(VALID_PASSWORD)))
                .isInstanceOf(TenantNotFoundException.class);
    }

    @Test
    void rejectsWhenTenantIsInactive() {
        Tenant inactiveTenant = Tenant.create(TENANT_ID, "Travesia Natural").deactivate();
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(inactiveTenant));

        assertThatThrownBy(() -> registerCollaboratorService.registerCollaborator(aCommand(VALID_PASSWORD)))
                .isInstanceOf(TenantInactiveException.class);
    }

    @Test
    void rejectsWeakPassword() {
        Tenant tenant = Tenant.create(TENANT_ID, "Travesia Natural");
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(tenant));

        assertThatThrownBy(() -> registerCollaboratorService.registerCollaborator(aCommand("weak")))
                .isInstanceOf(InvalidTenantException.class);

        verify(membershipRepositoryPort, never()).save(any());
    }

    @Test
    void rejectsWhenEmailAlreadyRegisteredInTenant() {
        Tenant tenant = Tenant.create(TENANT_ID, "Travesia Natural");
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(tenant));
        when(membershipRepositoryPort.existsByTenantIdAndEmail(TENANT_ID, EMAIL)).thenReturn(true);

        assertThatThrownBy(() -> registerCollaboratorService.registerCollaborator(aCommand(VALID_PASSWORD)))
                .isInstanceOf(EmailAlreadyRegisteredException.class);
    }
}
