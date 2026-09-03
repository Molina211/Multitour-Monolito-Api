package com.corhuila.errorcapa8.travesia_natural.common.security;

/**
 * Claims of a validated JWT (spec 007), used as the {@code Authentication} principal
 * once {@link JwtAuthenticationFilter} accepts a token. {@code membershipId} stays a
 * {@code String} (not {@code UUID}) because that is exactly how it travels in the
 * token's {@code sub} claim and how {@code Reservation.customerId()} already stores it.
 */
public record JwtPrincipal(String membershipId, String tenantId, String email, String role) {
}
