package com.corhuila.errorcapa8.travesia_natural.reservations.infrastructure.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "reservation_companions")
public class CompanionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "reservation_id", nullable = false)
    private ReservationEntity reservation;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "document", nullable = false)
    private String document;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    protected CompanionEntity() {
        // JPA
    }

    public CompanionEntity(String tenantId, String name, String document, LocalDate birthDate) {
        this.tenantId = tenantId;
        this.name = name;
        this.document = document;
        this.birthDate = birthDate;
    }

    void assignTo(ReservationEntity reservation) {
        this.reservation = reservation;
    }

    public Long getId() {
        return id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getName() {
        return name;
    }

    public String getDocument() {
        return document;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }
}
