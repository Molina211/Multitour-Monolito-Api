package com.corhuila.errorcapa8.travesia_natural.tenants.domain.model;

/**
 * Confirmed role catalog (02-domain/entities-and-rules.md, "Aggregate: Membership and identity").
 * Only ADMINISTRATOR is assignable in this cut (spec 002, first Administrator at tenant creation).
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
