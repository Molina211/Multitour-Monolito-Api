package com.corhuila.errorcapa8.travesia_natural.tenants.application;

import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.EmailAlreadyRegisteredException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.InvalidTenantException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantInactiveException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.Membership;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.Tenant;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.in.RegisterCustomerCommand;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterCustomerServiceTest {

    private static final String TENANT_ID = "travesia-natural";
    private static final String EMAIL = "cliente@correo.com";
    private static final String VALID_PASSWORD = "Password1!";

    @Mock
    private TenantRepositoryPort tenantRepositoryPort;
    @Mock
    private MembershipRepositoryPort membershipRepositoryPort;
    @Mock
    private PasswordEncoder passwordEncoder;

    private RegisterCustomerService registerCustomerService;

    @BeforeEach
    void setUp() {
        registerCustomerService = new RegisterCustomerService(tenantRepositoryPort, membershipRepositoryPort,
                passwordEncoder);
    }

    private RegisterCustomerCommand aCommand(String password) {
        return new RegisterCustomerCommand(TENANT_ID, "Ana", "Perez", EMAIL, "3001234567", password);
    }

    @Test
    void registersACustomerSuccessfully() {
        Tenant tenant = Tenant.create(TENANT_ID, "Travesia Natural");
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(tenant));
        when(membershipRepositoryPort.existsByTenantIdAndEmail(TENANT_ID, EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(VALID_PASSWORD)).thenReturn("hashed-password");
        when(membershipRepositoryPort.save(org.mockito.ArgumentMatchers.any(Membership.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Membership saved = registerCustomerService.registerCustomer(aCommand(VALID_PASSWORD));

        ArgumentCaptor<Membership> captor = ArgumentCaptor.forClass(Membership.class);
        verify(membershipRepositoryPort).save(captor.capture());
        assertThat(captor.getValue().email()).isEqualTo(EMAIL);
        assertThat(captor.getValue().passwordHash()).isEqualTo("hashed-password");
        assertThat(saved.email()).isEqualTo(EMAIL);
    }

    @Test
    void rejectsWhenTenantDoesNotExist() {
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> registerCustomerService.registerCustomer(aCommand(VALID_PASSWORD)))
                .isInstanceOf(TenantNotFoundException.class);

        verify(membershipRepositoryPort, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsWhenTenantIsInactive() {
        Tenant inactiveTenant = Tenant.create(TENANT_ID, "Travesia Natural").deactivate();
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(inactiveTenant));

        assertThatThrownBy(() -> registerCustomerService.registerCustomer(aCommand(VALID_PASSWORD)))
                .isInstanceOf(TenantInactiveException.class);
    }

    @Test
    void rejectsWeakPassword() {
        Tenant tenant = Tenant.create(TENANT_ID, "Travesia Natural");
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(tenant));

        assertThatThrownBy(() -> registerCustomerService.registerCustomer(aCommand("weak")))
                .isInstanceOf(InvalidTenantException.class);

        verify(membershipRepositoryPort, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsWhenEmailAlreadyRegisteredInTenant() {
        Tenant tenant = Tenant.create(TENANT_ID, "Travesia Natural");
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(tenant));
        when(membershipRepositoryPort.existsByTenantIdAndEmail(TENANT_ID, EMAIL)).thenReturn(true);

        assertThatThrownBy(() -> registerCustomerService.registerCustomer(aCommand(VALID_PASSWORD)))
                .isInstanceOf(EmailAlreadyRegisteredException.class);

        verify(membershipRepositoryPort, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
