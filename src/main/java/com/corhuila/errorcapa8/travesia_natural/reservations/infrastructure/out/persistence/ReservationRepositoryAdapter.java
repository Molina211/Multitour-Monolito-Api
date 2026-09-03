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

    @Override
    public Reservation save(Reservation reservation) {
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
                reservation.transferSupportReference());

        for (ReservedService reservedService : reservation.reservedServices()) {
            entity.addReservedService(new ReservedServiceEntity(
                    reservation.tenantId(),
                    reservedService.serviceReference(),
                    reservedService.partySize(),
                    reservedService.scheduledDate()));
        }

        jpaRepository.save(entity);

        return reservation;
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
                entity.getTransferSupportReference());
    }
}
