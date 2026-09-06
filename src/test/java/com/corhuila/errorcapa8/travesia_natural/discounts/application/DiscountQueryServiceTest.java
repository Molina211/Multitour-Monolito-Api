package com.corhuila.errorcapa8.travesia_natural.discounts.application;

import com.corhuila.errorcapa8.travesia_natural.discounts.domain.exception.DiscountNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.discounts.domain.model.Discount;
import com.corhuila.errorcapa8.travesia_natural.discounts.domain.model.DiscountBase;
import com.corhuila.errorcapa8.travesia_natural.discounts.domain.port.out.DiscountRepositoryPort;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.out.TenantRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiscountQueryServiceTest {

    private static final String TENANT_ID = "travesia-natural";

    @Mock
    private TenantRepositoryPort tenantRepositoryPort;
    @Mock
    private DiscountRepositoryPort discountRepositoryPort;

    private DiscountQueryService discountQueryService;

    @BeforeEach
    void setUp() {
        discountQueryService = new DiscountQueryService(tenantRepositoryPort, discountRepositoryPort);
    }

    private Discount aDiscount() {
        return Discount.create(TENANT_ID, UUID.randomUUID(), 10, null, null, 1, false, null,
                DiscountBase.ORIGINAL_VALUE);
    }

    @Test
    void getsADiscountById() {
        UUID discountId = UUID.randomUUID();
        Discount discount = aDiscount();
        when(tenantRepositoryPort.existsById(TENANT_ID)).thenReturn(true);
        when(discountRepositoryPort.findByTenantIdAndDiscountId(TENANT_ID, discountId))
                .thenReturn(Optional.of(discount));

        assertThat(discountQueryService.getById(TENANT_ID, discountId)).isEqualTo(discount);
    }

    @Test
    void rejectsWhenTenantDoesNotExist() {
        UUID discountId = UUID.randomUUID();
        when(tenantRepositoryPort.existsById(TENANT_ID)).thenReturn(false);

        assertThatThrownBy(() -> discountQueryService.getById(TENANT_ID, discountId))
                .isInstanceOf(TenantNotFoundException.class);
    }

    @Test
    void rejectsWhenDiscountDoesNotExist() {
        UUID discountId = UUID.randomUUID();
        when(tenantRepositoryPort.existsById(TENANT_ID)).thenReturn(true);
        when(discountRepositoryPort.findByTenantIdAndDiscountId(TENANT_ID, discountId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> discountQueryService.getById(TENANT_ID, discountId))
                .isInstanceOf(DiscountNotFoundException.class);
    }

    @Test
    void listsDiscountsOfATenant() {
        Discount discount = aDiscount();
        when(tenantRepositoryPort.existsById(TENANT_ID)).thenReturn(true);
        when(discountRepositoryPort.findAllByTenantId(TENANT_ID)).thenReturn(List.of(discount));

        assertThat(discountQueryService.listByTenant(TENANT_ID)).containsExactly(discount);
    }
}
