package com.corhuila.errorcapa8.travesia_natural.catalog.infrastructure.out.persistence;

import com.corhuila.errorcapa8.travesia_natural.catalog.domain.model.CatalogItem;
import com.corhuila.errorcapa8.travesia_natural.catalog.domain.model.CatalogItemType;
import com.corhuila.errorcapa8.travesia_natural.catalog.domain.port.out.CatalogItemRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class CatalogItemRepositoryAdapter implements CatalogItemRepositoryPort {

    private final CatalogItemJpaRepository jpaRepository;

    public CatalogItemRepositoryAdapter(CatalogItemJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public CatalogItem save(CatalogItem catalogItem) {
        jpaRepository.save(new CatalogItemEntity(
                catalogItem.catalogItemId(),
                catalogItem.tenantId(),
                catalogItem.type().name(),
                catalogItem.name(),
                catalogItem.price(),
                catalogItem.capacity(),
                catalogItem.restrictions(),
                catalogItem.validFrom(),
                catalogItem.validTo(),
                catalogItem.policy(),
                catalogItem.image(),
                catalogItem.active(),
                catalogItem.createdAt()));

        return catalogItem;
    }

    @Override
    public Optional<CatalogItem> findByTenantIdAndCatalogItemId(String tenantId, UUID catalogItemId) {
        return jpaRepository.findByTenantIdAndCatalogItemId(tenantId, catalogItemId)
                .map(CatalogItemRepositoryAdapter::toDomain);
    }

    @Override
    public List<CatalogItem> findAllByTenantId(String tenantId) {
        return jpaRepository.findAllByTenantId(tenantId).stream()
                .map(CatalogItemRepositoryAdapter::toDomain)
                .toList();
    }

    private static CatalogItem toDomain(CatalogItemEntity entity) {
        return CatalogItem.reconstitute(
                entity.getCatalogItemId(),
                entity.getTenantId(),
                CatalogItemType.valueOf(entity.getItemType()),
                entity.getName(),
                entity.getPrice(),
                entity.getCapacity(),
                entity.getRestrictions(),
                entity.getValidFrom(),
                entity.getValidTo(),
                entity.getPolicy(),
                entity.getImage(),
                entity.isActive(),
                entity.getCreatedAt());
    }
}
