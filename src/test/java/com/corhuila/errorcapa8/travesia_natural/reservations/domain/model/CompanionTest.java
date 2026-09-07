package com.corhuila.errorcapa8.travesia_natural.reservations.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CompanionTest {

    @Test
    void createsWithAllFields() {
        Companion companion = new Companion("Jane Doe", "123456", LocalDate.of(1990, 1, 1));

        assertThat(companion.name()).isEqualTo("Jane Doe");
        assertThat(companion.document()).isEqualTo("123456");
        assertThat(companion.birthDate()).isEqualTo(LocalDate.of(1990, 1, 1));
    }

    @Test
    void rejectsBlankName() {
        assertThatThrownBy(() -> new Companion("  ", "123456", LocalDate.of(1990, 1, 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("companion name is required");
    }

    @Test
    void rejectsNullDocument() {
        assertThatThrownBy(() -> new Companion("Jane Doe", null, LocalDate.of(1990, 1, 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("companion document is required");
    }

    @Test
    void rejectsNullBirthDate() {
        assertThatThrownBy(() -> new Companion("Jane Doe", "123456", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("companion birthDate is required");
    }
}
