package com.corhuila.errorcapa8.travesia_natural.tenants.infrastructure.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MembershipJpaRepository extends JpaRepository<MembershipEntity, UUID> {

    boolean existsByTenantIdAndEmail(String tenantId, String email);
}
