package com.corhuila.errorcapa8.travesia_natural.reservations.infrastructure.out.persistence;

import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.Reservation;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.ReservedService;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.out.ReservationRepositoryPort;
import org.springframework.stereotype.Component;

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
                reservation.createdAt());

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
}
