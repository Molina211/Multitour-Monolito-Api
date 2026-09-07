package com.corhuila.errorcapa8.travesia_natural.establishments.domain.model;

import com.corhuila.errorcapa8.travesia_natural.establishments.domain.exception.InvalidEstablishmentException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AssociatedEstablishmentTest {

    private static final String TENANT_ID = "travesia-natural";

    @Test
    void createsAnActiveEstablishment() {
        AssociatedEstablishment establishment = AssociatedEstablishment.create(TENANT_ID, EstablishmentKind.HOTEL,
                "Hotel Central", "descripcion", null);

        assertThat(establishment.kind()).isEqualTo(EstablishmentKind.HOTEL);
        assertThat(establishment.name()).isEqualTo("Hotel Central");
        assertThat(establishment.active()).isTrue();
    }

    @Test
    void createRejectsBlankTenantId() {
        assertThatThrownBy(() -> AssociatedEstablishment.create(" ", EstablishmentKind.HOTEL, "Hotel Central",
                null, null))
                .isInstanceOf(InvalidEstablishmentException.class)
                .hasMessageContaining("tenantId is required");
    }

    @Test
    void createRejectsNullKind() {
        assertThatThrownBy(() -> AssociatedEstablishment.create(TENANT_ID, null, "Hotel Central", null, null))
                .isInstanceOf(InvalidEstablishmentException.class)
                .hasMessageContaining("kind is required");
    }

    @Test
    void createRejectsBlankName() {
        assertThatThrownBy(() -> AssociatedEstablishment.create(TENANT_ID, EstablishmentKind.HOTEL, " ", null,
                null))
                .isInstanceOf(InvalidEstablishmentException.class)
                .hasMessageContaining("name is required");
    }

    @Test
    void deactivateRejectsAlreadyInactiveEstablishment() {
        AssociatedEstablishment inactive = AssociatedEstablishment.create(TENANT_ID, EstablishmentKind.RESTAURANT,
                "Restaurante Central", null, null).deactivate();

        assertThatThrownBy(inactive::deactivate)
                .isInstanceOf(InvalidEstablishmentException.class)
                .hasMessageContaining("already inactive");
    }

    @Test
    void reactivateRejectsAlreadyActiveEstablishment() {
        AssociatedEstablishment active = AssociatedEstablishment.create(TENANT_ID, EstablishmentKind.RESTAURANT,
                "Restaurante Central", null, null);

        assertThatThrownBy(active::reactivate)
                .isInstanceOf(InvalidEstablishmentException.class)
                .hasMessageContaining("already active");
    }

    @Test
    void reactivateBringsBackAnInactiveEstablishment() {
        AssociatedEstablishment inactive = AssociatedEstablishment.create(TENANT_ID, EstablishmentKind.RESTAURANT,
                "Restaurante Central", null, null).deactivate();

        assertThat(inactive.reactivate().active()).isTrue();
    }
}
