package com.corhuila.errorcapa8.travesia_natural.operations.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExecutionTest {

    private static final String TENANT_ID = "travesia-natural";

    @Test
    void createsAServedExecutionWithExecutedCountAndNoCausal() {
        UUID reservationId = UUID.randomUUID();

        Execution execution = Execution.create(TENANT_ID, reservationId, true, 4, null, "actor-1");

        assertThat(execution.served()).isTrue();
        assertThat(execution.executed()).isEqualTo(4);
        assertThat(execution.causal()).isNull();
    }

    @Test
    void createsANotServedExecutionWithCausalAndNoExecutedCount() {
        Execution execution = Execution.create(TENANT_ID, UUID.randomUUID(), false, 4, "clima adverso", "actor-1");

        assertThat(execution.served()).isFalse();
        assertThat(execution.executed()).isNull();
        assertThat(execution.causal()).isEqualTo("clima adverso");
    }

    @Test
    void createRejectsBlankTenantId() {
        assertThatThrownBy(() -> Execution.create(" ", UUID.randomUUID(), true, 1, null, "actor-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tenantId is required");
    }

    @Test
    void createRejectsNullReservationId() {
        assertThatThrownBy(() -> Execution.create(TENANT_ID, null, true, 1, null, "actor-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reservationId is required");
    }

    @Test
    void createRequiresCausalWhenNotServed() {
        assertThatThrownBy(() -> Execution.create(TENANT_ID, UUID.randomUUID(), false, null, " ", "actor-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("causal is required when the service was not served");
    }
}
