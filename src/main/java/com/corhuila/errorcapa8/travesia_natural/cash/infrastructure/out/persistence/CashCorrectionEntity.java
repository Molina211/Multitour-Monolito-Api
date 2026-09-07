package com.corhuila.errorcapa8.travesia_natural.cash.infrastructure.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "cash_corrections")
public class CashCorrectionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "cash_register_id", nullable = false)
    private CashRegisterEntity cashRegister;

    @Column(name = "justification", nullable = false)
    private String justification;

    @Column(name = "applied_by", nullable = false)
    private String appliedBy;

    @Column(name = "applied_at", nullable = false)
    private Instant appliedAt;

    protected CashCorrectionEntity() {
        // JPA
    }

    public CashCorrectionEntity(String justification, String appliedBy, Instant appliedAt) {
        this.justification = justification;
        this.appliedBy = appliedBy;
        this.appliedAt = appliedAt;
    }

    void assignTo(CashRegisterEntity cashRegister) {
        this.cashRegister = cashRegister;
    }

    public Long getId() {
        return id;
    }

    public String getJustification() {
        return justification;
    }

    public String getAppliedBy() {
        return appliedBy;
    }

    public Instant getAppliedAt() {
        return appliedAt;
    }
}
