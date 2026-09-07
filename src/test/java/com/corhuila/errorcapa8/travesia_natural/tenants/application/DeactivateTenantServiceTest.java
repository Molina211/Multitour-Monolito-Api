package com.corhuila.errorcapa8.travesia_natural.tenants.application;

import com.corhuila.errorcapa8.travesia_natural.common.audit.AuditRecord;
import com.corhuila.errorcapa8.travesia_natural.common.audit.AuditRecorder;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.InvalidTenantException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.Tenant;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.TenantStatus;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.in.DeactivateTenantCommand;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.out.TenantRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeactivateTenantServiceTest {

    private static final String TENANT_ID = "travesia-natural";

    @Mock
    private TenantRepositoryPort tenantRepositoryPort;
    @Mock
    private AuditRecorder auditRecorder;

    private DeactivateTenantService deactivateTenantService;

    @BeforeEach
    void setUp() {
        deactivateTenantService = new DeactivateTenantService(tenantRepositoryPort, auditRecorder);
    }

    @Test
    void deactivatesAnActiveTenantAndRecordsAudit() {
        Tenant tenant = Tenant.create(TENANT_ID, "Travesia Natural");
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(tenant));

        Tenant result = deactivateTenantService.deactivateTenant(
                new DeactivateTenantCommand(TENANT_ID, "incumplimiento", "actor-1"));

        assertThat(result.tenantStatus()).isEqualTo(TenantStatus.INACTIVO);
        verify(tenantRepositoryPort).save(result);

        ArgumentCaptor<AuditRecord> captor = ArgumentCaptor.forClass(AuditRecord.class);
        verify(auditRecorder).record(captor.capture());
        assertThat(captor.getValue().action()).isEqualTo("TENANT_DEACTIVATED");
    }

    @Test
    void rejectsBlankReason() {
        assertThatThrownBy(() -> deactivateTenantService.deactivateTenant(
                new DeactivateTenantCommand(TENANT_ID, " ", "actor-1")))
                .isInstanceOf(InvalidTenantException.class);

        verify(tenantRepositoryPort, never()).findById(any());
    }

    @Test
    void rejectsWhenTenantDoesNotExist() {
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deactivateTenantService.deactivateTenant(
                new DeactivateTenantCommand(TENANT_ID, "incumplimiento", "actor-1")))
                .isInstanceOf(TenantNotFoundException.class);
    }
}
