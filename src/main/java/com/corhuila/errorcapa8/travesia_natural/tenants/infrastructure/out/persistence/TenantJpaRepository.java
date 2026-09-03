package com.corhuila.errorcapa8.travesia_natural.tenants.infrastructure.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantJpaRepository extends JpaRepository<TenantEntity, String> {
}
