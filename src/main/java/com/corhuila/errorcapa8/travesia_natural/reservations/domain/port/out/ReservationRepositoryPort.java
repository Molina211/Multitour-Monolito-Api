package com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.out;

import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.Reservation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservationRepositoryPort {

    Reservation save(Reservation reservation);

    Optional<Reservation> findByTenantIdAndReservationId(String tenantId, UUID reservationId);

    List<Reservation> findAllByTenantId(String tenantId);
}
