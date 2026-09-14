package com.corhuila.errorcapa8.travesia_natural.tenants.application;

import com.corhuila.errorcapa8.travesia_natural.common.audit.AuditRecord;
import com.corhuila.errorcapa8.travesia_natural.common.audit.AuditRecorder;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantAlreadyExistsException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.Membership;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.Tenant;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.in.CreateTenantCommand;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.out.MembershipRepositoryPort;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.out.TenantRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateTenantServiceTest {

    private static final String TENANT_ID = "travesia-natural";

    @Mock
    private TenantRepositoryPort tenantRepositoryPort;
    @Mock
    private MembershipRepositoryPort membershipRepositoryPort;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuditRecorder auditRecorder;

    private CreateTenantService createTenantService;

    @BeforeEach
    void setUp() {
        createTenantService = new CreateTenantService(tenantRepositoryPort, membershipRepositoryPort,
                passwordEncoder, auditRecorder);
    }

    private CreateTenantCommand aCommand() {
        return new CreateTenantCommand(TENANT_ID, "Travesia Natural", "admin@correo.com", "Password1!", "actor-1");
    }

    @Test
    void createsATenantWithItsAdministratorAndRecordsAudit() {
        when(tenantRepositoryPort.existsById(TENANT_ID)).thenReturn(false);
        when(passwordEncoder.encode("Password1!")).thenReturn("hashed-password");

        Tenant created = createTenantService.createTenant(aCommand());

        assertThat(created.tenantId()).isEqualTo(TENANT_ID);
        verify(tenantRepositoryPort).save(any(Tenant.class));

        ArgumentCaptor<Membership> membershipCaptor = ArgumentCaptor.forClass(Membership.class);
        verify(membershipRepositoryPort).save(membershipCaptor.capture());
        assertThat(membershipCaptor.getValue().email()).isEqualTo("admin@correo.com");
        assertThat(membershipCaptor.getValue().passwordHash()).isEqualTo("hashed-password");

        ArgumentCaptor<AuditRecord> auditCaptor = ArgumentCaptor.forClass(AuditRecord.class);
        verify(auditRecorder).record(auditCaptor.capture());
        assertThat(auditCaptor.getValue().tenantId()).isEqualTo(TENANT_ID);
        assertThat(auditCaptor.getValue().actorId()).isEqualTo("actor-1");
        assertThat(auditCaptor.getValue().action()).isEqualTo("TENANT_CREATED");
    }

    @Test
    void rejectsWhenTenantAlreadyExists() {
        when(tenantRepositoryPort.existsById(TENANT_ID)).thenReturn(true);

        assertThatThrownBy(() -> createTenantService.createTenant(aCommand()))
                .isInstanceOf(TenantAlreadyExistsException.class);

        verify(tenantRepositoryPort, never()).save(any());
        verify(membershipRepositoryPort, never()).save(any());
        verify(auditRecorder, never()).record(any());
    }
}
