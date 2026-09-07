package com.corhuila.errorcapa8.travesia_natural.cash.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CashMovementTest {

    @Test
    void createsWithAllFields() {
        Instant now = Instant.now();
        CashMovement movement = new CashMovement(CashMovementType.INGRESO, BigDecimal.TEN, "venta", "actor-1", now);

        assertThat(movement.type()).isEqualTo(CashMovementType.INGRESO);
        assertThat(movement.amount()).isEqualByComparingTo(BigDecimal.TEN);
        assertThat(movement.concept()).isEqualTo("venta");
        assertThat(movement.actorId()).isEqualTo("actor-1");
        assertThat(movement.recordedAt()).isEqualTo(now);
    }

    @Test
    void rejectsNullType() {
        assertThatThrownBy(() -> new CashMovement(null, BigDecimal.TEN, "venta", "actor-1", Instant.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("movement type is required");
    }

    @Test
    void rejectsNonPositiveAmount() {
        assertThatThrownBy(() -> new CashMovement(CashMovementType.INGRESO, BigDecimal.ZERO, "venta", "actor-1",
                Instant.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("movement amount must be a positive value");
    }

    @Test
    void rejectsBlankConcept() {
        assertThatThrownBy(() -> new CashMovement(CashMovementType.INGRESO, BigDecimal.TEN, " ", "actor-1",
                Instant.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("movement concept is required");
    }

    @Test
    void rejectsBlankActorId() {
        assertThatThrownBy(() -> new CashMovement(CashMovementType.INGRESO, BigDecimal.TEN, "venta", " ",
                Instant.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("movement actorId is required");
    }
}
