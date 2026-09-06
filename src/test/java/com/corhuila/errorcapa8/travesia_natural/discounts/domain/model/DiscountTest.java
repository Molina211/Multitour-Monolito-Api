package com.corhuila.errorcapa8.travesia_natural.discounts.domain.model;

import com.corhuila.errorcapa8.travesia_natural.discounts.domain.exception.InvalidDiscountException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DiscountTest {

    private static final String TENANT_ID = "travesia-natural";

    private static UUID catalogItemId() {
        return UUID.randomUUID();
    }

    // --- create ---

    @Test
    void createsAnActiveDiscount() {
        Discount discount = Discount.create(TENANT_ID, catalogItemId(), 10, null, null, 1, false, null,
                DiscountBase.ORIGINAL_VALUE);

        assertThat(discount.percentage()).isEqualTo(10);
        assertThat(discount.active()).isTrue();
        assertThat(discount.base()).isEqualTo(DiscountBase.ORIGINAL_VALUE);
    }

    @Test
    void createRejectsBlankTenantId() {
        assertThatThrownBy(() -> Discount.create(" ", catalogItemId(), 10, null, null, 1, false, null,
                DiscountBase.ORIGINAL_VALUE))
                .isInstanceOf(InvalidDiscountException.class)
                .hasMessageContaining("tenantId is required");
    }

    @Test
    void createRejectsNullCatalogItemId() {
        assertThatThrownBy(() -> Discount.create(TENANT_ID, null, 10, null, null, 1, false, null,
                DiscountBase.ORIGINAL_VALUE))
                .isInstanceOf(InvalidDiscountException.class)
                .hasMessageContaining("catalogItemId is required");
    }

    @Test
    void createRejectsNullBase() {
        assertThatThrownBy(() -> Discount.create(TENANT_ID, catalogItemId(), 10, null, null, 1, false, null, null))
                .isInstanceOf(InvalidDiscountException.class)
                .hasMessageContaining("base is required");
    }

    @Test
    void createRejectsPercentageOutOfRange() {
        assertThatThrownBy(() -> Discount.create(TENANT_ID, catalogItemId(), 0, null, null, 1, false, null,
                DiscountBase.ORIGINAL_VALUE))
                .isInstanceOf(InvalidDiscountException.class)
                .hasMessageContaining("percentage must be between 1 and 100");

        assertThatThrownBy(() -> Discount.create(TENANT_ID, catalogItemId(), 101, null, null, 1, false, null,
                DiscountBase.ORIGINAL_VALUE))
                .isInstanceOf(InvalidDiscountException.class);
    }

    @Test
    void createRejectsValidToBeforeValidFrom() {
        assertThatThrownBy(() -> Discount.create(TENANT_ID, catalogItemId(), 10, LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 5, 1), 1, false, null, DiscountBase.ORIGINAL_VALUE))
                .isInstanceOf(InvalidDiscountException.class)
                .hasMessageContaining("validTo cannot be before validFrom");
    }

    @Test
    void createRejectsNonPositiveCapWhenPresent() {
        assertThatThrownBy(() -> Discount.create(TENANT_ID, catalogItemId(), 10, null, null, 1, false,
                BigDecimal.ZERO, DiscountBase.ORIGINAL_VALUE))
                .isInstanceOf(InvalidDiscountException.class)
                .hasMessageContaining("cap must be positive when present");
    }

    @Test
    void createAllowsNullCap() {
        Discount discount = Discount.create(TENANT_ID, catalogItemId(), 10, null, null, 1, false, null,
                DiscountBase.ORIGINAL_VALUE);

        assertThat(discount.cap()).isNull();
    }

    // --- update ---

    @Test
    void updateReplacesOnlyProvidedFields() {
        Discount discount = Discount.create(TENANT_ID, catalogItemId(), 10, null, null, 1, false, null,
                DiscountBase.ORIGINAL_VALUE);

        Discount updated = discount.update(20, null, null, null, null, null, null);

        assertThat(updated.percentage()).isEqualTo(20);
        assertThat(updated.priority()).isEqualTo(1);
        assertThat(updated.base()).isEqualTo(DiscountBase.ORIGINAL_VALUE);
    }

    @Test
    void updateRevalidatesPercentage() {
        Discount discount = Discount.create(TENANT_ID, catalogItemId(), 10, null, null, 1, false, null,
                DiscountBase.ORIGINAL_VALUE);

        assertThatThrownBy(() -> discount.update(200, null, null, null, null, null, null))
                .isInstanceOf(InvalidDiscountException.class);
    }

    // --- deactivate / reactivate ---

    @Test
    void deactivateRejectsAlreadyInactiveDiscount() {
        Discount inactive = Discount.create(TENANT_ID, catalogItemId(), 10, null, null, 1, false, null,
                DiscountBase.ORIGINAL_VALUE).deactivate();

        assertThatThrownBy(inactive::deactivate)
                .isInstanceOf(InvalidDiscountException.class)
                .hasMessageContaining("already inactive");
    }

    @Test
    void reactivateRejectsAlreadyActiveDiscount() {
        Discount active = Discount.create(TENANT_ID, catalogItemId(), 10, null, null, 1, false, null,
                DiscountBase.ORIGINAL_VALUE);

        assertThatThrownBy(active::reactivate)
                .isInstanceOf(InvalidDiscountException.class)
                .hasMessageContaining("already active");
    }

    @Test
    void reactivateBringsBackAnInactiveDiscount() {
        Discount inactive = Discount.create(TENANT_ID, catalogItemId(), 10, null, null, 1, false, null,
                DiscountBase.ORIGINAL_VALUE).deactivate();

        assertThat(inactive.reactivate().active()).isTrue();
    }
}
