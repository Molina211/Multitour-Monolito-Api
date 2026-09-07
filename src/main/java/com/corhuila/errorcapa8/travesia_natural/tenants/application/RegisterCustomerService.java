package com.corhuila.errorcapa8.travesia_natural.tenants.application;

import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.EmailAlreadyRegisteredException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantInactiveException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.Membership;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.PasswordPolicy;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.Tenant;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.TenantStatus;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.in.RegisterCustomerCommand;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.in.RegisterCustomerUseCase;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.out.MembershipRepositoryPort;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.out.TenantRepositoryPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class RegisterCustomerService implements RegisterCustomerUseCase {

    private final TenantRepositoryPort tenantRepositoryPort;
    private final MembershipRepositoryPort membershipRepositoryPort;
    private final PasswordEncoder passwordEncoder;

    public RegisterCustomerService(TenantRepositoryPort tenantRepositoryPort,
                                    MembershipRepositoryPort membershipRepositoryPort,
                                    PasswordEncoder passwordEncoder) {
        this.tenantRepositoryPort = tenantRepositoryPort;
        this.membershipRepositoryPort = membershipRepositoryPort;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Membership registerCustomer(RegisterCustomerCommand command) {
        Tenant tenant = tenantRepositoryPort.findById(command.tenantId())
                .orElseThrow(() -> new TenantNotFoundException(command.tenantId()));

        if (tenant.tenantStatus() == TenantStatus.INACTIVO) {
            throw new TenantInactiveException(tenant.tenantId());
        }

        PasswordPolicy.validate(command.password());

        if (membershipRepositoryPort.existsByTenantIdAndEmail(tenant.tenantId(), command.email())) {
            throw new EmailAlreadyRegisteredException(tenant.tenantId(), command.email());
        }

        String passwordHash = passwordEncoder.encode(command.password());
        Membership customer = Membership.createEndCustomer(
                tenant.tenantId(), command.firstName(), command.lastName(), command.email(),
                command.phone(), passwordHash);

        return membershipRepositoryPort.save(customer);
    }
}
