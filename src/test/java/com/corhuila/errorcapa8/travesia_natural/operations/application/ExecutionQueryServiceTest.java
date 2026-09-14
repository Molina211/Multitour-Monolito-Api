package com.corhuila.errorcapa8.travesia_natural.operations.application;

import com.corhuila.errorcapa8.travesia_natural.operations.domain.exception.ExecutionNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.operations.domain.model.Execution;
import com.corhuila.errorcapa8.travesia_natural.operations.domain.port.out.ExecutionRepositoryPort;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantInactiveException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.Tenant;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.out.TenantRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExecutionQueryServiceTest {

    private static final String TENANT_ID = "travesia-natural";

    @Mock
    private TenantRepositoryPort tenantRepositoryPort;
    @Mock
    private ExecutionRepositoryPort executionRepositoryPort;

    private ExecutionQueryService executionQueryService;

    @BeforeEach
    void setUp() {
        executionQueryService = new ExecutionQueryService(tenantRepositoryPort, executionRepositoryPort);
    }

    @Test
    void getsExecutionByReservation() {
        UUID reservationId = UUID.randomUUID();
        Execution execution = Execution.create(TENANT_ID, reservationId, true, 10, null, "actor-1");
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(Tenant.create(TENANT_ID, "Travesia")));
        when(executionRepositoryPort.findByTenantIdAndReservationId(TENANT_ID, reservationId))
                .thenReturn(Optional.of(execution));

        assertThat(executionQueryService.getByReservation(TENANT_ID, reservationId)).isEqualTo(execution);
    }

    @Test
    void rejectsWhenTenantDoesNotExist() {
        UUID reservationId = UUID.randomUUID();
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> executionQueryService.getByReservation(TENANT_ID, reservationId))
                .isInstanceOf(TenantNotFoundException.class);
    }

    @Test
    void rejectsWhenTenantIsInactive() {
        UUID reservationId = UUID.randomUUID();
        Tenant inactiveTenant = Tenant.create(TENANT_ID, "Travesia").deactivate();
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(inactiveTenant));

        assertThatThrownBy(() -> executionQueryService.getByReservation(TENANT_ID, reservationId))
                .isInstanceOf(TenantInactiveException.class);
    }

    @Test
    void rejectsWhenExecutionDoesNotExist() {
        UUID reservationId = UUID.randomUUID();
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(Tenant.create(TENANT_ID, "Travesia")));
        when(executionRepositoryPort.findByTenantIdAndReservationId(TENANT_ID, reservationId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> executionQueryService.getByReservation(TENANT_ID, reservationId))
                .isInstanceOf(ExecutionNotFoundException.class);
    }
}
