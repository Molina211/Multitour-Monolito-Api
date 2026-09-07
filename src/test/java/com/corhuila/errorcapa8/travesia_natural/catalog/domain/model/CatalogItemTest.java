package com.corhuila.errorcapa8.travesia_natural.catalog.domain.model;

import com.corhuila.errorcapa8.travesia_natural.catalog.domain.exception.InvalidCatalogItemException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CatalogItemTest {

    private static final String TENANT_ID = "travesia-natural";

    // --- create ---

    @Test
    void createsAnActiveTourWithoutCapacity() {
        CatalogItem item = CatalogItem.create(TENANT_ID, CatalogItemType.TOUR, "Laguna Verde",
                BigDecimal.valueOf(100), null, null, null, null, null, null, null, null);

        assertThat(item.type()).isEqualTo(CatalogItemType.TOUR);
        assertThat(item.name()).isEqualTo("Laguna Verde");
        assertThat(item.active()).isTrue();
        assertThat(item.capacity()).isNull();
    }

    @Test
    void createRejectsBlankTenantId() {
        assertThatThrownBy(() -> CatalogItem.create(" ", CatalogItemType.TOUR, "Laguna Verde", BigDecimal.TEN,
                null, null, null, null, null, null, null, null))
                .isInstanceOf(InvalidCatalogItemException.class)
                .hasMessageContaining("tenantId is required");
    }

    @Test
    void createRejectsNullType() {
        assertThatThrownBy(() -> CatalogItem.create(TENANT_ID, null, "Laguna Verde", BigDecimal.TEN,
                null, null, null, null, null, null, null, null))
                .isInstanceOf(InvalidCatalogItemException.class)
                .hasMessageContaining("type is required");
    }

    @Test
    void createRejectsBlankName() {
        assertThatThrownBy(() -> CatalogItem.create(TENANT_ID, CatalogItemType.TOUR, " ", BigDecimal.TEN,
                null, null, null, null, null, null, null, null))
                .isInstanceOf(InvalidCatalogItemException.class)
                .hasMessageContaining("name is required");
    }

    @Test
    void createRejectsNullPrice() {
        assertThatThrownBy(() -> CatalogItem.create(TENANT_ID, CatalogItemType.TOUR, "Laguna Verde", null,
                null, null, null, null, null, null, null, null))
                .isInstanceOf(InvalidCatalogItemException.class)
                .hasMessageContaining("price is required");
    }

    @Test
    void createLodgingRequiresPositiveCapacity() {
        assertThatThrownBy(() -> CatalogItem.create(TENANT_ID, CatalogItemType.LODGING, "Hotel Central",
                BigDecimal.TEN, null, null, null, null, null, null, null, null))
                .isInstanceOf(InvalidCatalogItemException.class)
                .hasMessageContaining("capacity is required and must be positive for LODGING items");

        assertThatThrownBy(() -> CatalogItem.create(TENANT_ID, CatalogItemType.LODGING, "Hotel Central",
                BigDecimal.TEN, 0, null, null, null, null, null, null, null))
                .isInstanceOf(InvalidCatalogItemException.class);
    }

    @Test
    void createLodgingWithPositiveCapacitySucceeds() {
        CatalogItem item = CatalogItem.create(TENANT_ID, CatalogItemType.LODGING, "Hotel Central", BigDecimal.TEN,
                10, null, null, null, null, null, null, null);

        assertThat(item.capacity()).isEqualTo(10);
    }

    @Test
    void createTourOrFoodDoesNotRequireCapacity() {
        CatalogItem tour = CatalogItem.create(TENANT_ID, CatalogItemType.TOUR, "Laguna Verde", BigDecimal.TEN,
                null, null, null, null, null, null, null, null);
        CatalogItem food = CatalogItem.create(TENANT_ID, CatalogItemType.FOOD, "Almuerzo tipico", BigDecimal.TEN,
                null, null, null, null, null, null, null, null);

        assertThat(tour.capacity()).isNull();
        assertThat(food.capacity()).isNull();
    }

    // --- update ---

    @Test
    void updateReplacesOnlyProvidedFields() {
        CatalogItem item = CatalogItem.create(TENANT_ID, CatalogItemType.TOUR, "Laguna Verde", BigDecimal.TEN,
                null, "sin restricciones", null, null, null, null, null, null);

        CatalogItem updated = item.update("Laguna Verde Full Day", null, null, null, null, null, null, null, null,
                null);

        assertThat(updated.name()).isEqualTo("Laguna Verde Full Day");
        assertThat(updated.price()).isEqualByComparingTo(BigDecimal.TEN);
        assertThat(updated.restrictions()).isEqualTo("sin restricciones");
    }

    @Test
    void updateRejectsBlankName() {
        CatalogItem item = CatalogItem.create(TENANT_ID, CatalogItemType.TOUR, "Laguna Verde", BigDecimal.TEN,
                null, null, null, null, null, null, null, null);

        assertThatThrownBy(() -> item.update(" ", null, null, null, null, null, null, null, null, null))
                .isInstanceOf(InvalidCatalogItemException.class)
                .hasMessageContaining("name is required");
    }

    @Test
    void updateRevalidatesLodgingCapacity() {
        CatalogItem item = CatalogItem.create(TENANT_ID, CatalogItemType.LODGING, "Hotel Central", BigDecimal.TEN,
                10, null, null, null, null, null, null, null);

        assertThatThrownBy(() -> item.update(null, null, 0, null, null, null, null, null, null, null))
                .isInstanceOf(InvalidCatalogItemException.class)
                .hasMessageContaining("capacity is required and must be positive for LODGING items");
    }

    // --- deactivate / reactivate ---

    @Test
    void deactivateMovesActiveItemToInactive() {
        CatalogItem item = CatalogItem.create(TENANT_ID, CatalogItemType.TOUR, "Laguna Verde", BigDecimal.TEN,
                null, null, null, null, null, null, null, null);

        assertThat(item.deactivate().active()).isFalse();
    }

    @Test
    void deactivateRejectsAlreadyInactiveItem() {
        CatalogItem inactive = CatalogItem.create(TENANT_ID, CatalogItemType.TOUR, "Laguna Verde", BigDecimal.TEN,
                null, null, null, null, null, null, null, null).deactivate();

        assertThatThrownBy(inactive::deactivate)
                .isInstanceOf(InvalidCatalogItemException.class)
                .hasMessageContaining("already inactive");
    }

    @Test
    void reactivateMovesInactiveItemToActive() {
        CatalogItem inactive = CatalogItem.create(TENANT_ID, CatalogItemType.TOUR, "Laguna Verde", BigDecimal.TEN,
                null, null, null, null, null, null, null, null).deactivate();

        assertThat(inactive.reactivate().active()).isTrue();
    }

    @Test
    void reactivateRejectsAlreadyActiveItem() {
        CatalogItem active = CatalogItem.create(TENANT_ID, CatalogItemType.TOUR, "Laguna Verde", BigDecimal.TEN,
                null, null, null, null, null, null, null, null);

        assertThatThrownBy(active::reactivate)
                .isInstanceOf(InvalidCatalogItemException.class)
                .hasMessageContaining("already active");
    }
}
