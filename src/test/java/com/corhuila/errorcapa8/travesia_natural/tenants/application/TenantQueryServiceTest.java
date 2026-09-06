package com.corhuila.errorcapa8.travesia_natural.tenants.application;

import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.Tenant;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.out.TenantRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantQueryServiceTest {

    private static final String TENANT_ID = "travesia-natural";

    @Mock
    private TenantRepositoryPort tenantRepositoryPort;

    private TenantQueryService tenantQueryService;

    @BeforeEach
    void setUp() {
        tenantQueryService = new TenantQueryService(tenantRepositoryPort);
    }

    @Test
    void getsATenantById() {
        Tenant tenant = Tenant.create(TENANT_ID, "Travesia Natural");
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(tenant));

        assertThat(tenantQueryService.getById(TENANT_ID)).isEqualTo(tenant);
    }

    @Test
    void rejectsWhenTenantDoesNotExist() {
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tenantQueryService.getById(TENANT_ID))
                .isInstanceOf(TenantNotFoundException.class);
    }

    @Test
    void listsAllTenants() {
        Tenant tenant = Tenant.create(TENANT_ID, "Travesia Natural");
        when(tenantRepositoryPort.findAll()).thenReturn(List.of(tenant));

        assertThat(tenantQueryService.listAll()).containsExactly(tenant);
    }
}
