package com.corhuila.errorcapa8.travesia_natural.tenants.domain.model;

import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.InvalidTenantException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MembershipTest {

    private static final String TENANT_ID = "travesia-natural";

    // --- createAdministrator ---

    @Test
    void createsAdministratorWithoutFirstLastNameOrPhone() {
        Membership administrator = Membership.createAdministrator(TENANT_ID, "admin@tenant.com", "hashed");

        assertThat(administrator.tenantId()).isEqualTo(TENANT_ID);
        assertThat(administrator.email()).isEqualTo("admin@tenant.com");
        assertThat(administrator.passwordHash()).isEqualTo("hashed");
        assertThat(administrator.role()).isEqualTo(MembershipRole.ADMINISTRATOR);
        assertThat(administrator.membershipStatus()).isEqualTo(MembershipStatus.ACTIVA);
        assertThat(administrator.firstName()).isNull();
        assertThat(administrator.lastName()).isNull();
        assertThat(administrator.phone()).isNull();
        assertThat(administrator.membershipId()).isNotNull();
    }

    @Test
    void createAdministratorRejectsBlankTenantId() {
        assertThatThrownBy(() -> Membership.createAdministrator(" ", "admin@tenant.com", "hashed"))
                .isInstanceOf(InvalidTenantException.class)
                .hasMessageContaining("tenantId is required");
    }

    @Test
    void createAdministratorRejectsBlankEmail() {
        assertThatThrownBy(() -> Membership.createAdministrator(TENANT_ID, " ", "hashed"))
                .isInstanceOf(InvalidTenantException.class)
                .hasMessageContaining("administrator email is required");
    }

    @Test
    void createAdministratorRejectsBlankPasswordHash() {
        assertThatThrownBy(() -> Membership.createAdministrator(TENANT_ID, "admin@tenant.com", " "))
                .isInstanceOf(InvalidTenantException.class)
                .hasMessageContaining("administrator password is required");
    }

    // --- createEndCustomer ---

    @Test
    void createsEndCustomerWithOptionalPhone() {
        Membership customer = Membership.createEndCustomer(
                TENANT_ID, "Jane", "Doe", "jane@doe.com", null, "hashed");

        assertThat(customer.role()).isEqualTo(MembershipRole.END_CUSTOMER);
        assertThat(customer.firstName()).isEqualTo("Jane");
        assertThat(customer.lastName()).isEqualTo("Doe");
        assertThat(customer.phone()).isNull();
    }

    @Test
    void createEndCustomerRejectsBlankFirstName() {
        assertThatThrownBy(() -> Membership.createEndCustomer(TENANT_ID, " ", "Doe", "jane@doe.com", null, "hashed"))
                .isInstanceOf(InvalidTenantException.class)
                .hasMessageContaining("firstName is required");
    }

    @Test
    void createEndCustomerRejectsBlankLastName() {
        assertThatThrownBy(() -> Membership.createEndCustomer(TENANT_ID, "Jane", " ", "jane@doe.com", null, "hashed"))
                .isInstanceOf(InvalidTenantException.class)
                .hasMessageContaining("lastName is required");
    }

    @Test
    void createEndCustomerRejectsBlankEmail() {
        assertThatThrownBy(() -> Membership.createEndCustomer(TENANT_ID, "Jane", "Doe", " ", null, "hashed"))
                .isInstanceOf(InvalidTenantException.class)
                .hasMessageContaining("email is required");
    }

    @Test
    void createEndCustomerRejectsBlankPasswordHash() {
        assertThatThrownBy(() -> Membership.createEndCustomer(TENANT_ID, "Jane", "Doe", "jane@doe.com", null, " "))
                .isInstanceOf(InvalidTenantException.class)
                .hasMessageContaining("password is required");
    }

    // --- createOperationalCollaborator ---

    @Test
    void createsOperationalCollaboratorWithFullNameInFirstName() {
        Membership collaborator = Membership.createOperationalCollaborator(
                TENANT_ID, "Full Name", "collab@tenant.com", "hashed");

        assertThat(collaborator.role()).isEqualTo(MembershipRole.OPERATIONAL_COLLABORATOR);
        assertThat(collaborator.firstName()).isEqualTo("Full Name");
        assertThat(collaborator.lastName()).isNull();
        assertThat(collaborator.phone()).isNull();
    }

    @Test
    void createOperationalCollaboratorRejectsBlankName() {
        assertThatThrownBy(() -> Membership.createOperationalCollaborator(TENANT_ID, " ", "collab@tenant.com",
                "hashed"))
                .isInstanceOf(InvalidTenantException.class)
                .hasMessageContaining("collaborator name is required");
    }

    @Test
    void createOperationalCollaboratorRejectsBlankEmail() {
        assertThatThrownBy(() -> Membership.createOperationalCollaborator(TENANT_ID, "Full Name", " ", "hashed"))
                .isInstanceOf(InvalidTenantException.class)
                .hasMessageContaining("collaborator email is required");
    }

    @Test
    void createOperationalCollaboratorRejectsBlankPasswordHash() {
        assertThatThrownBy(() -> Membership.createOperationalCollaborator(TENANT_ID, "Full Name",
                "collab@tenant.com", " "))
                .isInstanceOf(InvalidTenantException.class)
                .hasMessageContaining("collaborator password is required");
    }

    // --- createPlatformAdministrator ---

    @Test
    void createsPlatformAdministratorWithoutFirstLastNameOrPhone() {
        Membership platformAdmin = Membership.createPlatformAdministrator(
                "platform", "admin@multitour.plataforma", "hashed");

        assertThat(platformAdmin.role()).isEqualTo(MembershipRole.PLATFORM_ADMINISTRATOR);
        assertThat(platformAdmin.tenantId()).isEqualTo("platform");
        assertThat(platformAdmin.firstName()).isNull();
        assertThat(platformAdmin.lastName()).isNull();
        assertThat(platformAdmin.phone()).isNull();
    }

    @Test
    void createPlatformAdministratorRejectsBlankTenantId() {
        assertThatThrownBy(() -> Membership.createPlatformAdministrator(" ", "admin@multitour.plataforma", "hashed"))
                .isInstanceOf(InvalidTenantException.class)
                .hasMessageContaining("tenantId is required");
    }

    @Test
    void createPlatformAdministratorRejectsBlankEmail() {
        assertThatThrownBy(() -> Membership.createPlatformAdministrator("platform", " ", "hashed"))
                .isInstanceOf(InvalidTenantException.class)
                .hasMessageContaining("platform administrator email is required");
    }

    @Test
    void createPlatformAdministratorRejectsBlankPasswordHash() {
        assertThatThrownBy(() -> Membership.createPlatformAdministrator(
                "platform", "admin@multitour.plataforma", " "))
                .isInstanceOf(InvalidTenantException.class)
                .hasMessageContaining("platform administrator password is required");
    }

    // --- reconstitute ---

    @Test
    void reconstituteDoesNotReValidateInvariants() {
        UUID membershipId = UUID.randomUUID();
        Instant createdAt = Instant.now();

        Membership membership = Membership.reconstitute(membershipId, TENANT_ID, null, null,
                "admin@tenant.com", null, "hashed", MembershipRole.ADMINISTRATOR, MembershipStatus.INACTIVA,
                createdAt);

        assertThat(membership.membershipId()).isEqualTo(membershipId);
        assertThat(membership.membershipStatus()).isEqualTo(MembershipStatus.INACTIVA);
        assertThat(membership.createdAt()).isEqualTo(createdAt);
    }
}
