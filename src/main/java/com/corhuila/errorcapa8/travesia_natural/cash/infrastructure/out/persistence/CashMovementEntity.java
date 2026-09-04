package com.corhuila.errorcapa8.travesia_natural.cash.infrastructure.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "cash_movements")
public class CashMovementEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "cash_register_id", nullable = false)
    private CashRegisterEntity cashRegister;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "concept", nullable = false)
    private String concept;

    @Column(name = "actor_id", nullable = false)
    private String actorId;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    protected CashMovementEntity() {
        // JPA
    }

    public CashMovementEntity(String type, BigDecimal amount, String concept, String actorId, Instant recordedAt) {
        this.type = type;
        this.amount = amount;
        this.concept = concept;
        this.actorId = actorId;
        this.recordedAt = recordedAt;
    }

    void assignTo(CashRegisterEntity cashRegister) {
        this.cashRegister = cashRegister;
    }

    public Long getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getConcept() {
        return concept;
    }

    public String getActorId() {
        return actorId;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }
}
