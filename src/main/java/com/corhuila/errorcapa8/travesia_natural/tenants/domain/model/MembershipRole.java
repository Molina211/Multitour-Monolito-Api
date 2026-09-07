package com.corhuila.errorcapa8.travesia_natural.tenants.domain.model;

/**
 * Confirmed role catalog (02-domain/entities-and-rules.md, "Aggregate: Membership and identity").
 * ADMINISTRATOR, OPERATIONAL_COLLABORATOR and END_CUSTOMER are assignable through their own
 * registration flows (specs 002, 003, 014). PLATFORM_ADMINISTRATOR has no registration HU; it
 * only exists via the one-time seed in {@code PlatformAdministratorSeeder}.
 */
public enum MembershipRole {
    PLATFORM_ADMINISTRATOR,
    ADMINISTRATOR,
    OPERATIONAL_COLLABORATOR,
    END_CUSTOMER,
    MANAGER,
    ACCOUNTANT,
    ANALYST
}
