package com.corhuila.errorcapa8.travesia_natural.reservations.infrastructure.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "reserved_services")
public class ReservedServiceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "reservation_id", nullable = false)
    private ReservationEntity reservation;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "service_reference", nullable = false)
    private String serviceReference;

    @Column(name = "party_size")
    private Integer partySize;

    @Column(name = "scheduled_date")
    private LocalDate scheduledDate;

    @Column(name = "transport_item_id")
    private UUID transportItemId;

    @Column(name = "transport_cost")
    private BigDecimal transportCost;

    protected ReservedServiceEntity() {
        // JPA
    }

    public ReservedServiceEntity(String tenantId, String serviceReference, Integer partySize, LocalDate scheduledDate,
                                  UUID transportItemId, BigDecimal transportCost) {
        this.tenantId = tenantId;
        this.serviceReference = serviceReference;
        this.partySize = partySize;
        this.scheduledDate = scheduledDate;
        this.transportItemId = transportItemId;
        this.transportCost = transportCost;
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

    public String getServiceReference() {
        return serviceReference;
    }

    public Integer getPartySize() {
        return partySize;
    }

    public LocalDate getScheduledDate() {
        return scheduledDate;
    }

    public UUID getTransportItemId() {
        return transportItemId;
    }

    public BigDecimal getTransportCost() {
        return transportCost;
    }
}
