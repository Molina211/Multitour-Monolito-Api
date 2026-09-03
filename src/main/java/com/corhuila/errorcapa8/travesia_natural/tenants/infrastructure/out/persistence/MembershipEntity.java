package com.corhuila.errorcapa8.travesia_natural.tenants.infrastructure.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "memberships")
public class MembershipEntity {

    @Id
    @Column(name = "membership_id")
    private UUID membershipId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "role", nullable = false)
    private String role;

    @Column(name = "membership_status", nullable = false)
    private String membershipStatus;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected MembershipEntity() {
        // JPA
    }

    public MembershipEntity(UUID membershipId, String tenantId, String email, String passwordHash, String role,
                             String membershipStatus, Instant createdAt) {
        this.membershipId = membershipId;
        this.tenantId = tenantId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.membershipStatus = membershipStatus;
        this.createdAt = createdAt;
    }

    public UUID getMembershipId() {
        return membershipId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getRole() {
        return role;
    }

    public String getMembershipStatus() {
        return membershipStatus;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
