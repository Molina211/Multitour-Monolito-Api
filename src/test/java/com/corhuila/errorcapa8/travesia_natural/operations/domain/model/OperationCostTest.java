package com.corhuila.errorcapa8.travesia_natural.operations.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OperationCostTest {

    private static final String TENANT_ID = "travesia-natural";

    @Test
    void createsWithAllFields() {
        UUID reservationId = UUID.randomUUID();

        OperationCost cost = OperationCost.create(TENANT_ID, reservationId, "combustible", BigDecimal.TEN,
                "actor-1");

        assertThat(cost.reservationId()).isEqualTo(reservationId);
        assertThat(cost.concept()).isEqualTo("combustible");
        assertThat(cost.amount()).isEqualByComparingTo(BigDecimal.TEN);
        assertThat(cost.actorId()).isEqualTo("actor-1");
    }

    @Test
    void createRejectsBlankTenantId() {
        assertThatThrownBy(() -> OperationCost.create(" ", UUID.randomUUID(), "combustible", BigDecimal.TEN,
                "actor-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tenantId is required");
    }

    @Test
    void createRejectsNullReservationId() {
        assertThatThrownBy(() -> OperationCost.create(TENANT_ID, null, "combustible", BigDecimal.TEN, "actor-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reservationId is required");
    }

    @Test
    void createRejectsBlankConcept() {
        assertThatThrownBy(() -> OperationCost.create(TENANT_ID, UUID.randomUUID(), " ", BigDecimal.TEN, "actor-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("concept is required");
    }

    @Test
    void createRejectsNonPositiveAmount() {
        assertThatThrownBy(() -> OperationCost.create(TENANT_ID, UUID.randomUUID(), "combustible", BigDecimal.ZERO,
                "actor-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("amount must be a positive value");
    }
}
