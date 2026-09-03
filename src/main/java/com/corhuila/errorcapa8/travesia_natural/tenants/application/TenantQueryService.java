package com.corhuila.errorcapa8.travesia_natural.tenants.application;

import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.Tenant;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.in.TenantQueryUseCase;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.out.TenantRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TenantQueryService implements TenantQueryUseCase {

    private final TenantRepositoryPort tenantRepositoryPort;

    public TenantQueryService(TenantRepositoryPort tenantRepositoryPort) {
        this.tenantRepositoryPort = tenantRepositoryPort;
    }

    @Override
    public Tenant getById(String tenantId) {
        return tenantRepositoryPort.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));
    }

    @Override
    public List<Tenant> listAll() {
        return tenantRepositoryPort.findAll();
    }
}
