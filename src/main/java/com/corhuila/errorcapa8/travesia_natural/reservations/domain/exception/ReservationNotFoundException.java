package com.corhuila.errorcapa8.travesia_natural.reservations.domain.exception;

public class ReservationNotFoundException extends RuntimeException {

    public ReservationNotFoundException(String reservationId) {
        super("reservation not found: " + reservationId);
    }
}
