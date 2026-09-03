package com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.out;

import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.Membership;

public interface MembershipRepositoryPort {

    Membership save(Membership membership);

    boolean existsByTenantIdAndEmail(String tenantId, String email);
}
