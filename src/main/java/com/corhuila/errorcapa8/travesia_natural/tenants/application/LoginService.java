package com.corhuila.errorcapa8.travesia_natural.tenants.application;

import com.corhuila.errorcapa8.travesia_natural.common.security.JwtTokenProvider;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.InvalidCredentialsException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.Membership;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.MembershipStatus;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.Tenant;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.TenantStatus;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.in.LoginCommand;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.in.LoginResult;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.in.LoginUseCase;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.out.MembershipRepositoryPort;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.out.TenantRepositoryPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class LoginService implements LoginUseCase {

    private final TenantRepositoryPort tenantRepositoryPort;
    private final MembershipRepositoryPort membershipRepositoryPort;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public LoginService(TenantRepositoryPort tenantRepositoryPort,
                         MembershipRepositoryPort membershipRepositoryPort,
                         PasswordEncoder passwordEncoder,
                         JwtTokenProvider jwtTokenProvider) {
        this.tenantRepositoryPort = tenantRepositoryPort;
        this.membershipRepositoryPort = membershipRepositoryPort;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public LoginResult login(LoginCommand command) {
        // Every rejection below throws the exact same InvalidCredentialsException, on
        // purpose (HU-IAM-002 escenarios 2 y 3): a nonexistent tenant, an Inactivo
        // tenant, an email with no membership in this tenant, an INACTIVA membership
        // and a wrong password are all indistinguishable to the caller. See the
        // extensive comment in AuthController for why the tenantId must come from the
        // URL in the first place.
        Tenant tenant = tenantRepositoryPort.findById(command.tenantId())
                .orElseThrow(InvalidCredentialsException::new);

        if (tenant.tenantStatus() == TenantStatus.INACTIVO) {
            throw new InvalidCredentialsException();
        }

        Membership membership = membershipRepositoryPort.findByTenantIdAndEmail(tenant.tenantId(), command.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (membership.membershipStatus() != MembershipStatus.ACTIVA) {
            throw new InvalidCredentialsException();
        }

        if (!passwordEncoder.matches(command.password(), membership.passwordHash())) {
            throw new InvalidCredentialsException();
        }

        String accessToken = jwtTokenProvider.generateToken(
                membership.membershipId(), membership.tenantId(), membership.email(), membership.role().name());

        return new LoginResult(accessToken, membership);
    }
}
