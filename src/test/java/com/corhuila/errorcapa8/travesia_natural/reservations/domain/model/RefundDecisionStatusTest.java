package com.corhuila.errorcapa8.travesia_natural.reservations.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RefundDecisionStatusTest {

    @ParameterizedTest
    @EnumSource(RefundDecisionStatus.class)
    void fromLabelRoundTripsForEveryValue(RefundDecisionStatus status) {
        assertThat(RefundDecisionStatus.fromLabel(status.label())).isEqualTo(status);
    }

    @Test
    void fromLabelRejectsUnknownLabel() {
        assertThatThrownBy(() -> RefundDecisionStatus.fromLabel("no existe"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown RefundDecisionStatus label");
    }
}
