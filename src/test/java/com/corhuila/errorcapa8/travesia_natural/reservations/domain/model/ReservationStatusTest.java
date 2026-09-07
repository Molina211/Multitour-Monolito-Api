package com.corhuila.errorcapa8.travesia_natural.reservations.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservationStatusTest {

    @ParameterizedTest
    @EnumSource(ReservationStatus.class)
    void fromLabelRoundTripsForEveryValue(ReservationStatus status) {
        assertThat(ReservationStatus.fromLabel(status.label())).isEqualTo(status);
    }

    @Test
    void fromLabelRejectsUnknownLabel() {
        assertThatThrownBy(() -> ReservationStatus.fromLabel("no existe"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown ReservationStatus label");
    }
}
