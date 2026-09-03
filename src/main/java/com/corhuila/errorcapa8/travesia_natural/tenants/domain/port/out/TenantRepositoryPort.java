package com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.out;

import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.Tenant;

import java.util.List;
import java.util.Optional;

public interface TenantRepositoryPort {

    Tenant save(Tenant tenant);

    Optional<Tenant> findById(String tenantId);

    boolean existsById(String tenantId);

    List<Tenant> findAll();
}
