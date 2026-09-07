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

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "phone")
    private String phone;

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

    public MembershipEntity(UUID membershipId, String tenantId, String firstName, String lastName, String email,
                             String phone, String passwordHash, String role, String membershipStatus,
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

    public UUID getMembershipId() {
        return membershipId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
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
