package com.corhuila.errorcapa8.travesia_natural.tenants.infrastructure.out.persistence;

import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.Membership;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.out.MembershipRepositoryPort;
import org.springframework.stereotype.Component;

@Component
public class MembershipRepositoryAdapter implements MembershipRepositoryPort {

    private final MembershipJpaRepository jpaRepository;

    public MembershipRepositoryAdapter(MembershipJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Membership save(Membership membership) {
        jpaRepository.save(new MembershipEntity(
                membership.membershipId(),
                membership.tenantId(),
                membership.firstName(),
                membership.lastName(),
                membership.email(),
                membership.phone(),
                membership.passwordHash(),
                membership.role().name(),
                membership.membershipStatus().name(),
                membership.createdAt()));

        return membership;
    }

    @Override
    public boolean existsByTenantIdAndEmail(String tenantId, String email) {
        return jpaRepository.existsByTenantIdAndEmail(tenantId, email);
    }
}
