package com.corhuila.errorcapa8.travesia_natural.tenants.infrastructure.out.persistence;

import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.Tenant;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.TenantStatus;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.out.TenantRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class TenantRepositoryAdapter implements TenantRepositoryPort {

    private final TenantJpaRepository jpaRepository;

    public TenantRepositoryAdapter(TenantJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Tenant save(Tenant tenant) {
        jpaRepository.save(new TenantEntity(
                tenant.tenantId(),
                tenant.commercialName(),
                tenant.tenantStatus().name(),
                tenant.createdAt()));

        return tenant;
    }

    @Override
    public Optional<Tenant> findById(String tenantId) {
        return jpaRepository.findById(tenantId).map(TenantRepositoryAdapter::toDomain);
    }

    @Override
    public boolean existsById(String tenantId) {
        return jpaRepository.existsById(tenantId);
    }

    @Override
    public List<Tenant> findAll() {
        return jpaRepository.findAll().stream().map(TenantRepositoryAdapter::toDomain).toList();
    }

    private static Tenant toDomain(TenantEntity entity) {
        return Tenant.reconstitute(
                entity.getTenantId(),
                entity.getCommercialName(),
                TenantStatus.valueOf(entity.getTenantStatus()),
                entity.getCreatedAt());
    }
}
