package com.corhuila.errorcapa8.travesia_natural.cash.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CashCorrectionTest {

    @Test
    void createsWithAllFields() {
        Instant now = Instant.now();
        CashCorrection correction = new CashCorrection("ajuste por error de digitacion", "actor-1", now);

        assertThat(correction.justification()).isEqualTo("ajuste por error de digitacion");
        assertThat(correction.appliedBy()).isEqualTo("actor-1");
        assertThat(correction.appliedAt()).isEqualTo(now);
    }

    @Test
    void rejectsBlankJustification() {
        assertThatThrownBy(() -> new CashCorrection(" ", "actor-1", Instant.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("correction justification is required");
    }

    @Test
    void rejectsBlankAppliedBy() {
        assertThatThrownBy(() -> new CashCorrection("ajuste", " ", Instant.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("correction appliedBy is required");
    }
}
