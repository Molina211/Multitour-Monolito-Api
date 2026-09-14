package com.corhuila.errorcapa8.travesia_natural.reservations.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservedServiceTest {

    @Test
    void createsWithAllFields() {
        UUID transportItemId = UUID.randomUUID();
        ReservedService service = new ReservedService(
                "tour-laguna-verde", 2, LocalDate.of(2026, 12, 1), transportItemId, BigDecimal.TEN);

        assertThat(service.serviceReference()).isEqualTo("tour-laguna-verde");
        assertThat(service.partySize()).isEqualTo(2);
        assertThat(service.scheduledDate()).isEqualTo(LocalDate.of(2026, 12, 1));
        assertThat(service.transportItemId()).isEqualTo(transportItemId);
        assertThat(service.transportCost()).isEqualTo(BigDecimal.TEN);
    }

    @Test
    void allowsNullOptionalFields() {
        ReservedService service = new ReservedService("tour-laguna-verde", null, null, null, null);

        assertThat(service.partySize()).isNull();
        assertThat(service.scheduledDate()).isNull();
        assertThat(service.transportItemId()).isNull();
        assertThat(service.transportCost()).isNull();
    }

    @Test
    void rejectsNullServiceReference() {
        assertThatThrownBy(() -> new ReservedService(null, 1, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("serviceReference is required");
    }

    @Test
    void rejectsBlankServiceReference() {
        assertThatThrownBy(() -> new ReservedService("   ", 1, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("serviceReference is required");
    }
}
