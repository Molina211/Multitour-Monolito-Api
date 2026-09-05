package com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.out;

import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.Membership;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.MembershipRole;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MembershipRepositoryPort {

    Membership save(Membership membership);

    boolean existsByTenantIdAndEmail(String tenantId, String email);

    Optional<Membership> findByTenantIdAndEmail(String tenantId, String email);

    List<Membership> findAllByTenantIdAndRole(String tenantId, MembershipRole role);

    Optional<Membership> findByTenantIdAndMembershipId(String tenantId, UUID membershipId);
}
