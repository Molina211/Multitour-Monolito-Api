package com.corhuila.errorcapa8.travesia_natural.tenants.domain.model;

import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.InvalidTenantException;

import java.time.Instant;
import java.util.UUID;

/**
 * Aggregate root for the Identity and Access bounded context (02-domain/entities-and-rules.md,
 * "Aggregate: Membership and identity"). Credentials are embedded per membership in this cut
 * (spec 002 plan, decision: no global Identity/User entity yet).
 */
public final class Membership {

    private final UUID membershipId;
    private final String tenantId;
    private final String firstName;
    private final String lastName;
    private final String email;
    private final String phone;
    private final String passwordHash;
    private final MembershipRole role;
    private final MembershipStatus membershipStatus;
    private final Instant createdAt;

    private Membership(UUID membershipId, String tenantId, String firstName, String lastName, String email,
                        String phone, String passwordHash, MembershipRole role, MembershipStatus membershipStatus,
                        Instant createdAt) {
        this.membershipId = membershipId;
        this.tenantId = tenantId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.passwordHash = passwordHash;
        this.role = role;
        this.membershipStatus = membershipStatus;
        this.createdAt = createdAt;
    }

    /**
     * Creates the first Administrator membership of a tenant (HU-TEN-001 escenario 1).
     * {@code passwordHash} must already be hashed by the caller (application layer, via
     * PasswordEncoder) — this factory never sees a plain-text password. Administrator
     * memberships have no first/last name or phone in this cut (spec 002 scope).
     */
    public static Membership createAdministrator(String tenantId, String email, String passwordHash) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new InvalidTenantException("tenantId is required");
        }
        if (email == null || email.isBlank()) {
            throw new InvalidTenantException("administrator email is required");
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new InvalidTenantException("administrator password is required");
        }

        return new Membership(
                UUID.randomUUID(),
                tenantId,
                null,
                null,
                email,
                null,
                passwordHash,
                MembershipRole.ADMINISTRATOR,
                MembershipStatus.ACTIVA,
                Instant.now());
    }

    /**
     * Creates an End Customer membership (HU-IAM-001). {@code passwordHash} must already be
     * hashed by the caller (application layer, via PasswordEncoder). {@code phone} is optional.
     */
    public static Membership createEndCustomer(String tenantId, String firstName, String lastName, String email,
                                                 String phone, String passwordHash) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new InvalidTenantException("tenantId is required");
        }
        if (firstName == null || firstName.isBlank()) {
            throw new InvalidTenantException("firstName is required");
        }
        if (lastName == null || lastName.isBlank()) {
            throw new InvalidTenantException("lastName is required");
        }
        if (email == null || email.isBlank()) {
            throw new InvalidTenantException("email is required");
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new InvalidTenantException("password is required");
        }

        return new Membership(
                UUID.randomUUID(),
                tenantId,
                firstName,
                lastName,
                email,
                phone,
                passwordHash,
                MembershipRole.END_CUSTOMER,
                MembershipStatus.ACTIVA,
                Instant.now());
    }

    /**
     * Creates an Operational Collaborator membership (spec 014, "Gestión de colaboradores
     * del operador"). {@code passwordHash} must already be hashed by the caller (application
     * layer, via PasswordEncoder). {@code name} is the collaborator's full name as entered by
     * the Administrator — stored in {@code firstName}; {@code lastName} and {@code phone} stay
     * {@code null}, same as {@code createAdministrator}, because the Frontend collects a single
     * "nombre completo" field, not first/last name separately.
     */
    public static Membership createOperationalCollaborator(String tenantId, String name, String email,
                                                             String passwordHash) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new InvalidTenantException("tenantId is required");
        }
        if (name == null || name.isBlank()) {
            throw new InvalidTenantException("collaborator name is required");
        }
        if (email == null || email.isBlank()) {
            throw new InvalidTenantException("collaborator email is required");
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new InvalidTenantException("collaborator password is required");
        }

        return new Membership(
                UUID.randomUUID(),
                tenantId,
                name,
                null,
                email,
                null,
                passwordHash,
                MembershipRole.OPERATIONAL_COLLABORATOR,
                MembershipStatus.ACTIVA,
                Instant.now());
    }

    /**
     * Rebuilds a Membership already persisted (spec 004, login). Unlike {@code createAdministrator}/
     * {@code createEndCustomer}, this factory does not enforce the creation-time invariants (e.g.
     * firstName required) because a row that already exists in the database was valid when it was
     * created under whichever role it has; re-validating it on every read would reject legitimate
     * Administrator rows (which never had firstName/lastName) if this used createEndCustomer, or
     * fail to carry role-specific fields if it used createAdministrator. Mirrors Tenant.reconstitute.
     */
    public static Membership reconstitute(UUID membershipId, String tenantId, String firstName, String lastName,
                                           String email, String phone, String passwordHash, MembershipRole role,
                                           MembershipStatus membershipStatus, Instant createdAt) {
        return new Membership(membershipId, tenantId, firstName, lastName, email, phone, passwordHash, role,
                membershipStatus, createdAt);
    }

    public UUID membershipId() {
        return membershipId;
    }

    public String tenantId() {
        return tenantId;
    }

    public String firstName() {
        return firstName;
    }

    public String lastName() {
        return lastName;
    }

    public String email() {
        return email;
    }

    public String phone() {
        return phone;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public MembershipRole role() {
        return role;
    }

    public MembershipStatus membershipStatus() {
        return membershipStatus;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
