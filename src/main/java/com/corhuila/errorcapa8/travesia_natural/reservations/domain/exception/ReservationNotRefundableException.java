package com.corhuila.errorcapa8.travesia_natural.reservations.domain.exception;

public class ReservationNotRefundableException extends RuntimeException {

    public ReservationNotRefundableException(String message) {
        super(message);
    }
}
