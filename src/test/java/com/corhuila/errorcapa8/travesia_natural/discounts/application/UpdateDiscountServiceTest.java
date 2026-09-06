package com.corhuila.errorcapa8.travesia_natural.discounts.application;

import com.corhuila.errorcapa8.travesia_natural.discounts.domain.exception.DiscountNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.discounts.domain.model.Discount;
import com.corhuila.errorcapa8.travesia_natural.discounts.domain.model.DiscountBase;
import com.corhuila.errorcapa8.travesia_natural.discounts.domain.port.in.UpdateDiscountCommand;
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
class UpdateDiscountServiceTest {

    private static final String TENANT_ID = "travesia-natural";

    @Mock
    private DiscountRepositoryPort discountRepositoryPort;

    private UpdateDiscountService updateDiscountService;

    @BeforeEach
    void setUp() {
        updateDiscountService = new UpdateDiscountService(discountRepositoryPort);
    }

    @Test
    void updatesADiscount() {
        UUID discountId = UUID.randomUUID();
        Discount discount = Discount.create(TENANT_ID, UUID.randomUUID(), 10, null, null, 1, false, null,
                DiscountBase.ORIGINAL_VALUE);
        when(discountRepositoryPort.findByTenantIdAndDiscountId(TENANT_ID, discountId))
                .thenReturn(Optional.of(discount));
        when(discountRepositoryPort.save(any(Discount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Discount result = updateDiscountService.updateDiscount(
                new UpdateDiscountCommand(TENANT_ID, discountId, 20, null, null, null, null, null, null));

        assertThat(result.percentage()).isEqualTo(20);
    }

    @Test
    void rejectsWhenDiscountDoesNotExist() {
        UUID discountId = UUID.randomUUID();
        when(discountRepositoryPort.findByTenantIdAndDiscountId(TENANT_ID, discountId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> updateDiscountService.updateDiscount(
                new UpdateDiscountCommand(TENANT_ID, discountId, 20, null, null, null, null, null, null)))
                .isInstanceOf(DiscountNotFoundException.class);
    }
}
