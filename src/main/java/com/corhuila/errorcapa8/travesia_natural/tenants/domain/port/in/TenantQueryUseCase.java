package com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.in;

import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.Tenant;

import java.util.List;

public interface TenantQueryUseCase {

    Tenant getById(String tenantId);

    List<Tenant> listAll();
}
