package com.corhuila.errorcapa8.travesia_natural.discounts.infrastructure.out.persistence;

import com.corhuila.errorcapa8.travesia_natural.discounts.domain.model.Discount;
import com.corhuila.errorcapa8.travesia_natural.discounts.domain.model.DiscountBase;
import com.corhuila.errorcapa8.travesia_natural.discounts.domain.port.out.DiscountRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class DiscountRepositoryAdapter implements DiscountRepositoryPort {

    private final DiscountJpaRepository jpaRepository;

    public DiscountRepositoryAdapter(DiscountJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Discount save(Discount discount) {
        jpaRepository.save(new DiscountEntity(
                discount.discountId(),
                discount.tenantId(),
                discount.catalogItemId(),
                discount.percentage(),
                discount.validFrom(),
                discount.validTo(),
                discount.priority(),
                discount.stackable(),
                discount.cap(),
                discount.base().name(),
                discount.active(),
                discount.createdAt()));

        return discount;
    }

    @Override
    public Optional<Discount> findByTenantIdAndDiscountId(String tenantId, UUID discountId) {
        return jpaRepository.findByTenantIdAndDiscountId(tenantId, discountId)
                .map(DiscountRepositoryAdapter::toDomain);
    }

    @Override
    public List<Discount> findAllByTenantId(String tenantId) {
        return jpaRepository.findAllByTenantId(tenantId).stream()
                .map(DiscountRepositoryAdapter::toDomain)
                .toList();
    }

    private static Discount toDomain(DiscountEntity entity) {
        return Discount.reconstitute(
                entity.getDiscountId(),
                entity.getTenantId(),
                entity.getCatalogItemId(),
                entity.getPercentage(),
                entity.getValidFrom(),
                entity.getValidTo(),
                entity.getPriority(),
                entity.isStackable(),
                entity.getCap(),
                DiscountBase.valueOf(entity.getBase()),
                entity.isActive(),
                entity.getCreatedAt());
    }
}
