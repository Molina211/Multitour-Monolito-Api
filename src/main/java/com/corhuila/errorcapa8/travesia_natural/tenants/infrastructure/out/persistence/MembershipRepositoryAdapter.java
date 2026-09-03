package com.corhuila.errorcapa8.travesia_natural.tenants.infrastructure.out.persistence;

import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.Membership;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.MembershipRole;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.MembershipStatus;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.out.MembershipRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.Optional;

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

    @Override
    public Optional<Membership> findByTenantIdAndEmail(String tenantId, String email) {
        return jpaRepository.findByTenantIdAndEmail(tenantId, email).map(MembershipRepositoryAdapter::toDomain);
    }

    private static Membership toDomain(MembershipEntity entity) {
        return Membership.reconstitute(
                entity.getMembershipId(),
                entity.getTenantId(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getEmail(),
                entity.getPhone(),
                entity.getPasswordHash(),
                MembershipRole.valueOf(entity.getRole()),
                MembershipStatus.valueOf(entity.getMembershipStatus()),
                entity.getCreatedAt());
    }
}
