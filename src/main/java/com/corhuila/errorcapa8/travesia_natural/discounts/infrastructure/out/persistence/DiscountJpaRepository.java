package com.corhuila.errorcapa8.travesia_natural.discounts.infrastructure.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DiscountJpaRepository extends JpaRepository<DiscountEntity, UUID> {

    Optional<DiscountEntity> findByTenantIdAndDiscountId(String tenantId, UUID discountId);

    List<DiscountEntity> findAllByTenantId(String tenantId);
}
