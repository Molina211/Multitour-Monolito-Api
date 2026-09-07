package com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.in;

import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.Membership;

import java.util.List;
import java.util.UUID;

public interface CollaboratorQueryUseCase {

    List<Membership> listByTenant(String tenantId);

    Membership getById(String tenantId, UUID membershipId);
}
