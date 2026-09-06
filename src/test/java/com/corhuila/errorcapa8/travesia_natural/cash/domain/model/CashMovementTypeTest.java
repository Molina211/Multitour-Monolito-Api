package com.corhuila.errorcapa8.travesia_natural.cash.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CashMovementTypeTest {

    @ParameterizedTest
    @EnumSource(CashMovementType.class)
    void fromLabelRoundTripsForEveryValue(CashMovementType type) {
        assertThat(CashMovementType.fromLabel(type.label())).isEqualTo(type);
    }

    @Test
    void fromLabelRejectsUnknownLabel() {
        assertThatThrownBy(() -> CashMovementType.fromLabel("no existe"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown CashMovementType label");
    }
}
