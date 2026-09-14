package com.corhuila.errorcapa8.travesia_natural.cash.application;

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

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantGuardTest {

    private static final String TENANT_ID = "travesia-natural";

    @Mock
    private TenantRepositoryPort tenantRepositoryPort;

    private TenantGuard tenantGuard;

    @BeforeEach
    void setUp() {
        tenantGuard = new TenantGuard(tenantRepositoryPort);
    }

    @Test
    void allowsAnActiveTenant() {
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(Tenant.create(TENANT_ID, "Travesia")));

        assertThatCode(() -> tenantGuard.requireActive(TENANT_ID)).doesNotThrowAnyException();
    }

    @Test
    void rejectsWhenTenantDoesNotExist() {
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tenantGuard.requireActive(TENANT_ID))
                .isInstanceOf(TenantNotFoundException.class);
    }

    @Test
    void rejectsWhenTenantIsInactive() {
        Tenant inactive = Tenant.create(TENANT_ID, "Travesia").deactivate();
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> tenantGuard.requireActive(TENANT_ID))
                .isInstanceOf(TenantInactiveException.class);
    }
}
