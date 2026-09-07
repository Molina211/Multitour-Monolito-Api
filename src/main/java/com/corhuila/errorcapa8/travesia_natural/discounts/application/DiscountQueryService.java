package com.corhuila.errorcapa8.travesia_natural.discounts.application;

import com.corhuila.errorcapa8.travesia_natural.discounts.domain.exception.DiscountNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.discounts.domain.model.Discount;
import com.corhuila.errorcapa8.travesia_natural.discounts.domain.port.in.DiscountQueryUseCase;
import com.corhuila.errorcapa8.travesia_natural.discounts.domain.port.out.DiscountRepositoryPort;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.out.TenantRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class DiscountQueryService implements DiscountQueryUseCase {

    private final TenantRepositoryPort tenantRepositoryPort;
    private final DiscountRepositoryPort discountRepositoryPort;

    public DiscountQueryService(TenantRepositoryPort tenantRepositoryPort,
                                 DiscountRepositoryPort discountRepositoryPort) {
        this.tenantRepositoryPort = tenantRepositoryPort;
        this.discountRepositoryPort = discountRepositoryPort;
    }

    @Override
    public Discount getById(String tenantId, UUID discountId) {
        requireTenant(tenantId);

        return discountRepositoryPort.findByTenantIdAndDiscountId(tenantId, discountId)
                .orElseThrow(() -> new DiscountNotFoundException(discountId.toString()));
    }

    @Override
    public List<Discount> listByTenant(String tenantId) {
        requireTenant(tenantId);

        return discountRepositoryPort.findAllByTenantId(tenantId);
    }

    private void requireTenant(String tenantId) {
        if (!tenantRepositoryPort.existsById(tenantId)) {
            throw new TenantNotFoundException(tenantId);
        }
    }
}
