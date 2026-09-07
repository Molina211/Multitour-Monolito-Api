package com.corhuila.errorcapa8.travesia_natural.tenants.application;

import com.corhuila.errorcapa8.travesia_natural.common.security.JwtTokenProvider;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.InvalidCredentialsException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.Membership;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.MembershipRole;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.MembershipStatus;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.Tenant;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.TenantStatus;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.in.LoginCommand;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.in.LoginResult;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.out.MembershipRepositoryPort;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.out.TenantRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    private static final String TENANT_ID = "travesia-natural";
    private static final String EMAIL = "cliente@correo.com";
    private static final String PASSWORD = "Password1!";

    @Mock
    private TenantRepositoryPort tenantRepositoryPort;
    @Mock
    private MembershipRepositoryPort membershipRepositoryPort;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private LoginService loginService;

    @BeforeEach
    void setUp() {
        loginService = new LoginService(tenantRepositoryPort, membershipRepositoryPort, passwordEncoder,
                jwtTokenProvider);
    }

    private Membership activeCustomer() {
        return Membership.reconstitute(UUID.randomUUID(), TENANT_ID, "Ana", "Perez", EMAIL, null,
                "hashed-password", MembershipRole.END_CUSTOMER, MembershipStatus.ACTIVA, Instant.now());
    }

    @Test
    void logsInSuccessfullyAndReturnsAccessToken() {
        Tenant tenant = Tenant.create(TENANT_ID, "Travesia Natural");
        Membership membership = activeCustomer();
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(tenant));
        when(membershipRepositoryPort.findByTenantIdAndEmail(TENANT_ID, EMAIL)).thenReturn(Optional.of(membership));
        when(passwordEncoder.matches(PASSWORD, membership.passwordHash())).thenReturn(true);
        when(jwtTokenProvider.generateToken(membership.membershipId(), membership.tenantId(), membership.email(),
                membership.role().name())).thenReturn("signed-jwt");

        LoginResult result = loginService.login(new LoginCommand(TENANT_ID, EMAIL, PASSWORD));

        assertThat(result.accessToken()).isEqualTo("signed-jwt");
        assertThat(result.membership()).isEqualTo(membership);
    }

    @Test
    void rejectsWhenTenantDoesNotExist() {
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loginService.login(new LoginCommand(TENANT_ID, EMAIL, PASSWORD)))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void rejectsWhenTenantIsInactive() {
        Tenant inactiveTenant = Tenant.create(TENANT_ID, "Travesia Natural").deactivate();
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(inactiveTenant));

        assertThatThrownBy(() -> loginService.login(new LoginCommand(TENANT_ID, EMAIL, PASSWORD)))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void rejectsWhenMembershipDoesNotExist() {
        Tenant tenant = Tenant.create(TENANT_ID, "Travesia Natural");
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(tenant));
        when(membershipRepositoryPort.findByTenantIdAndEmail(TENANT_ID, EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loginService.login(new LoginCommand(TENANT_ID, EMAIL, PASSWORD)))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void rejectsWhenMembershipIsInactive() {
        Tenant tenant = Tenant.create(TENANT_ID, "Travesia Natural");
        Membership inactiveMembership = Membership.reconstitute(UUID.randomUUID(), TENANT_ID, "Ana", "Perez", EMAIL,
                null, "hashed-password", MembershipRole.END_CUSTOMER, MembershipStatus.INACTIVA, Instant.now());
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(tenant));
        when(membershipRepositoryPort.findByTenantIdAndEmail(TENANT_ID, EMAIL))
                .thenReturn(Optional.of(inactiveMembership));

        assertThatThrownBy(() -> loginService.login(new LoginCommand(TENANT_ID, EMAIL, PASSWORD)))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void rejectsWhenPasswordDoesNotMatch() {
        Tenant tenant = Tenant.create(TENANT_ID, "Travesia Natural");
        Membership membership = activeCustomer();
        when(tenantRepositoryPort.findById(TENANT_ID)).thenReturn(Optional.of(tenant));
        when(membershipRepositoryPort.findByTenantIdAndEmail(TENANT_ID, EMAIL)).thenReturn(Optional.of(membership));
        when(passwordEncoder.matches(PASSWORD, membership.passwordHash())).thenReturn(false);

        assertThatThrownBy(() -> loginService.login(new LoginCommand(TENANT_ID, EMAIL, PASSWORD)))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(jwtTokenProvider, org.mockito.Mockito.never()).generateToken(any(), anyString(), anyString(),
                anyString());
    }
}
