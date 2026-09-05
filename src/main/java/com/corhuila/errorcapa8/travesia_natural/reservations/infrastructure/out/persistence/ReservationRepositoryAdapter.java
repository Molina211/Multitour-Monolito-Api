package com.corhuila.errorcapa8.travesia_natural.reservations.infrastructure.out.persistence;

import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.PaymentStatus;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.Reservation;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.ReservationStatus;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.ReservedService;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.out.ReservationRepositoryPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class ReservationRepositoryAdapter implements ReservationRepositoryPort {

    private final ReservationJpaRepository jpaRepository;

    public ReservationRepositoryAdapter(ReservationJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    /**
     * {@code reservedServices} se fija una sola vez al crear la reserva y ningún método de
     * dominio lo modifica después (pagar/cancelar/devolver solo cambian campos escalares):
     * si la reserva ya existe, se actualizan esos campos y no se toca la colección de
     * servicios (antes: se reconstruía el agregado completo en cada guardado, y
     * `orphanRemoval` borraba y reinsertaba los servicios reservados en cada pago o
     * cancelación, aunque no hubieran cambiado).
     */
    @Override
    @Transactional
    public Reservation save(Reservation reservation) {
        ReservationEntity entity = jpaRepository.findById(reservation.reservationId())
                .map(existing -> applyChanges(existing, reservation))
                .orElseGet(() -> toNewEntity(reservation));

        jpaRepository.save(entity);

        return reservation;
    }

    private static ReservationEntity applyChanges(ReservationEntity entity, Reservation reservation) {
        entity.updateState(
                reservation.finalValue(),
                reservation.pendingBalance(),
                reservation.creditBalance(),
                reservation.reservationStatus().label(),
                reservation.paymentStatus().label(),
                reservation.paymentMethod(),
                reservation.pendingTransferAmount(),
                reservation.transferSupportReference(),
                reservation.cancellationReason(),
                reservation.cancelledBy(),
                reservation.cancelledAt(),
                reservation.refundedAmount(),
                reservation.refundReason(),
                reservation.refundedBy(),
                reservation.refundMethod(),
                reservation.refundedAt());

        return entity;
    }

    private static ReservationEntity toNewEntity(Reservation reservation) {
        ReservationEntity entity = new ReservationEntity(
                reservation.reservationId(),
                reservation.tenantId(),
                reservation.customerId(),
                reservation.projectedValue(),
                reservation.finalValue(),
                reservation.pendingBalance(),
                reservation.creditBalance(),
                reservation.reservationStatus().label(),
                reservation.paymentStatus().label(),
                reservation.paymentMethod(),
                reservation.createdAt(),
                reservation.pendingTransferAmount(),
                reservation.transferSupportReference(),
                reservation.cancellationReason(),
                reservation.cancelledBy(),
                reservation.cancelledAt(),
                reservation.refundedAmount(),
                reservation.refundReason(),
                reservation.refundedBy(),
                reservation.refundMethod(),
                reservation.refundedAt());

        for (ReservedService reservedService : reservation.reservedServices()) {
            entity.addReservedService(new ReservedServiceEntity(
                    reservation.tenantId(),
                    reservedService.serviceReference(),
                    reservedService.partySize(),
                    reservedService.scheduledDate()));
        }

        return entity;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Reservation> findByTenantIdAndReservationId(String tenantId, UUID reservationId) {
        return jpaRepository.findByTenantIdAndReservationId(tenantId, reservationId)
                .map(ReservationRepositoryAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Reservation> findAllByTenantId(String tenantId) {
        return jpaRepository.findAllByTenantId(tenantId).stream()
                .map(ReservationRepositoryAdapter::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Reservation> findAllByTenantIdAndCustomerId(String tenantId, String customerId) {
        return jpaRepository.findAllByTenantIdAndCustomerId(tenantId, customerId).stream()
                .map(ReservationRepositoryAdapter::toDomain)
                .toList();
    }

    private static Reservation toDomain(ReservationEntity entity) {
        List<ReservedService> reservedServices = entity.getReservedServices().stream()
                .map(rs -> new ReservedService(rs.getServiceReference(), rs.getPartySize(), rs.getScheduledDate()))
                .toList();

        return Reservation.reconstitute(
                entity.getReservationId(),
                entity.getTenantId(),
                entity.getCustomerId(),
                reservedServices,
                entity.getProjectedValue(),
                entity.getFinalValue(),
                entity.getPendingBalance(),
                entity.getCreditBalance(),
                ReservationStatus.fromLabel(entity.getReservationStatus()),
                PaymentStatus.fromLabel(entity.getPaymentStatus()),
                entity.getPaymentMethod(),
                entity.getCreatedAt(),
                entity.getPendingTransferAmount(),
                entity.getTransferSupportReference(),
                entity.getCancellationReason(),
                entity.getCancelledBy(),
                entity.getCancelledAt(),
                entity.getRefundedAmount(),
                entity.getRefundReason(),
                entity.getRefundedBy(),
                entity.getRefundMethod(),
                entity.getRefundedAt());
    }
}
