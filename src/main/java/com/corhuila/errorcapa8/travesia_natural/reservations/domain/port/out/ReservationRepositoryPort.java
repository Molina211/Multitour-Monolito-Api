package com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.out;

import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.Reservation;

public interface ReservationRepositoryPort {

    Reservation save(Reservation reservation);
}
