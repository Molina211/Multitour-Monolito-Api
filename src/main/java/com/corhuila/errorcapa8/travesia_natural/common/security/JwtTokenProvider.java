package com.corhuila.errorcapa8.travesia_natural.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Issues and validates JWTs for spec 004 (End Customer login) and spec 007 (JWT
 * enforcement). Injected directly where needed, the same way {@code PasswordEncoder}
 * already is in this codebase (common/security), instead of behind a hexagonal port:
 * it is infrastructure shared across the whole application, not a domain concept that
 * {@code tenants} owns or that another module would need to swap out independently.
 */
@Component
public class JwtTokenProvider {

    private final SecretKey signingKey;
    private final long expirationMinutes;

    public JwtTokenProvider(@Value("${app.jwt.secret}") String secret,
                             @Value("${app.jwt.expiration-minutes}") long expirationMinutes) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;
    }

    public String generateToken(UUID membershipId, String tenantId, String email, String role) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(Duration.ofMinutes(expirationMinutes));

        return Jwts.builder()
                .subject(membershipId.toString())
                .claim("tenantId", tenantId)
                .claim("email", email)
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Validates signature and expiration and returns the claims as a {@link JwtPrincipal}.
     * Throws {@link JwtException} (e.g. {@code ExpiredJwtException}, {@code SignatureException})
     * on any invalid token — callers (spec 007's {@code JwtAuthenticationFilter}) decide what
     * to do with that, this method never swallows it.
     */
    public JwtPrincipal parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return new JwtPrincipal(
                claims.getSubject(),
                claims.get("tenantId", String.class),
                claims.get("email", String.class),
                claims.get("role", String.class));
    }
}
