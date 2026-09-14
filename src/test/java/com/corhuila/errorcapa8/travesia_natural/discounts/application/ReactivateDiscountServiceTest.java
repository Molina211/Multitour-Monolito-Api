package com.corhuila.errorcapa8.travesia_natural.discounts.application;

import com.corhuila.errorcapa8.travesia_natural.discounts.domain.exception.DiscountNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.discounts.domain.model.Discount;
import com.corhuila.errorcapa8.travesia_natural.discounts.domain.model.DiscountBase;
import com.corhuila.errorcapa8.travesia_natural.discounts.domain.port.out.DiscountRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReactivateDiscountServiceTest {

    private static final String TENANT_ID = "travesia-natural";

    @Mock
    private DiscountRepositoryPort discountRepositoryPort;

    private ReactivateDiscountService reactivateDiscountService;

    @BeforeEach
    void setUp() {
        reactivateDiscountService = new ReactivateDiscountService(discountRepositoryPort);
    }

    @Test
    void reactivatesADiscount() {
        UUID discountId = UUID.randomUUID();
        Discount inactive = Discount.create(TENANT_ID, UUID.randomUUID(), 10, null, null, 1, false, null,
                DiscountBase.ORIGINAL_VALUE).deactivate();
        when(discountRepositoryPort.findByTenantIdAndDiscountId(TENANT_ID, discountId))
                .thenReturn(Optional.of(inactive));
        when(discountRepositoryPort.save(any(Discount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Discount result = reactivateDiscountService.reactivateDiscount(TENANT_ID, discountId);

        assertThat(result.active()).isTrue();
    }

    @Test
    void rejectsWhenDiscountDoesNotExist() {
        UUID discountId = UUID.randomUUID();
        when(discountRepositoryPort.findByTenantIdAndDiscountId(TENANT_ID, discountId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reactivateDiscountService.reactivateDiscount(TENANT_ID, discountId))
                .isInstanceOf(DiscountNotFoundException.class);
    }
}
