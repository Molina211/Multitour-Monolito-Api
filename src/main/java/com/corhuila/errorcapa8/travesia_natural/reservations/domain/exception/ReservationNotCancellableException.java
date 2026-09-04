package com.corhuila.errorcapa8.travesia_natural.reservations.domain.exception;

public class ReservationNotCancellableException extends RuntimeException {

    public ReservationNotCancellableException(String message) {
        super(message);
    }
}
