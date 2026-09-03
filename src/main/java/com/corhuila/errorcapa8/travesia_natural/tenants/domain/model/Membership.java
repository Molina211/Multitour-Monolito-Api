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
    private final String email;
    private final String passwordHash;
    private final MembershipRole role;
    private final MembershipStatus membershipStatus;
    private final Instant createdAt;

    private Membership(UUID membershipId, String tenantId, String email, String passwordHash,
                        MembershipRole role, MembershipStatus membershipStatus, Instant createdAt) {
        this.membershipId = membershipId;
        this.tenantId = tenantId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.membershipStatus = membershipStatus;
        this.createdAt = createdAt;
    }

    /**
     * Creates the first Administrator membership of a tenant (HU-TEN-001 escenario 1).
     * {@code passwordHash} must already be hashed by the caller (application layer, via
     * PasswordEncoder) — this factory never sees a plain-text password.
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
                email,
                passwordHash,
                MembershipRole.ADMINISTRATOR,
                MembershipStatus.ACTIVA,
                Instant.now());
    }

    public UUID membershipId() {
        return membershipId;
    }

    public String tenantId() {
        return tenantId;
    }

    public String email() {
        return email;
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
