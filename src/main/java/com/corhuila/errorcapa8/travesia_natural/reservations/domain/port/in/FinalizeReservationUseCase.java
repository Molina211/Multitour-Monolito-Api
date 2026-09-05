package com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.in;

import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.Reservation;

public interface FinalizeReservationUseCase {

    Reservation finalizeReservation(FinalizeReservationCommand command);
}
