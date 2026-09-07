package com.corhuila.errorcapa8.travesia_natural.reservations.domain.exception;

public class PaymentAlreadyResolvedException extends RuntimeException {

    public PaymentAlreadyResolvedException(String message) {
        super(message);
    }
}
