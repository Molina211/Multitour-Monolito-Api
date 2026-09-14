package com.corhuila.errorcapa8.travesia_natural.tenants.domain.model;

import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.InvalidTenantException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantTest {

    // --- create ---

    @Test
    void createsAnActiveTenantWithoutCollaboratorSupportValidation() {
        Tenant tenant = Tenant.create("travesia-natural", "Travesia Natural");

        assertThat(tenant.tenantId()).isEqualTo("travesia-natural");
        assertThat(tenant.commercialName()).isEqualTo("Travesia Natural");
        assertThat(tenant.tenantStatus()).isEqualTo(TenantStatus.ACTIVO);
        assertThat(tenant.allowCollaboratorSupportValidation()).isFalse();
        assertThat(tenant.createdAt()).isNotNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"AB", "Tenant_1", "ab", "UPPERCASE", "with spaces", "tw"})
    void createRejectsTenantIdNotMatchingPattern(String invalidTenantId) {
        assertThatThrownBy(() -> Tenant.create(invalidTenantId, "Some Name"))
                .isInstanceOf(InvalidTenantException.class)
                .hasMessageContaining("tenantId must match");
    }

    @Test
    void createRejectsNullTenantId() {
        assertThatThrownBy(() -> Tenant.create(null, "Some Name"))
                .isInstanceOf(InvalidTenantException.class)
                .hasMessageContaining("tenantId must match");
    }

    @Test
    void createAcceptsLowercaseDigitsAndHyphens() {
        Tenant tenant = Tenant.create("tenant-2026", "Some Name");

        assertThat(tenant.tenantId()).isEqualTo("tenant-2026");
    }

    @Test
    void createRejectsBlankCommercialName() {
        assertThatThrownBy(() -> Tenant.create("travesia-natural", " "))
                .isInstanceOf(InvalidTenantException.class)
                .hasMessageContaining("commercialName is required");
    }

    // --- deactivate / reactivate ---

    @Test
    void deactivateMovesActiveTenantToInactivo() {
        Tenant active = Tenant.create("travesia-natural", "Travesia Natural");

        Tenant deactivated = active.deactivate();

        assertThat(deactivated.tenantStatus()).isEqualTo(TenantStatus.INACTIVO);
    }

    @Test
    void deactivateRejectsAlreadyInactiveTenant() {
        Tenant inactive = Tenant.create("travesia-natural", "Travesia Natural").deactivate();

        assertThatThrownBy(inactive::deactivate)
                .isInstanceOf(InvalidTenantException.class)
                .hasMessageContaining("already Inactivo");
    }

    @Test
    void reactivateMovesInactiveTenantToActivo() {
        Tenant inactive = Tenant.create("travesia-natural", "Travesia Natural").deactivate();

        Tenant reactivated = inactive.reactivate();

        assertThat(reactivated.tenantStatus()).isEqualTo(TenantStatus.ACTIVO);
    }

    @Test
    void reactivateRejectsAlreadyActiveTenant() {
        Tenant active = Tenant.create("travesia-natural", "Travesia Natural");

        assertThatThrownBy(active::reactivate)
                .isInstanceOf(InvalidTenantException.class)
                .hasMessageContaining("already Activo");
    }

    // --- updateCollaboratorSupportValidationPermission ---

    @Test
    void updatePermissionTogglesFlagWhenTenantIsActive() {
        Tenant active = Tenant.create("travesia-natural", "Travesia Natural");

        Tenant updated = active.updateCollaboratorSupportValidationPermission(true);

        assertThat(updated.allowCollaboratorSupportValidation()).isTrue();
    }

    @Test
    void updatePermissionRejectsWhenTenantIsInactive() {
        Tenant inactive = Tenant.create("travesia-natural", "Travesia Natural").deactivate();

        assertThatThrownBy(() -> inactive.updateCollaboratorSupportValidationPermission(true))
                .isInstanceOf(InvalidTenantException.class)
                .hasMessageContaining("must be Activo");
    }

    // --- reconstitute ---

    @Test
    void reconstituteDoesNotReValidateInvariants() {
        Instant createdAt = Instant.now();

        Tenant tenant = Tenant.reconstitute("travesia-natural", "Travesia Natural", TenantStatus.INACTIVO,
                createdAt, true);

        assertThat(tenant.tenantStatus()).isEqualTo(TenantStatus.INACTIVO);
        assertThat(tenant.allowCollaboratorSupportValidation()).isTrue();
        assertThat(tenant.createdAt()).isEqualTo(createdAt);
    }
}
