package com.corhuila.errorcapa8.travesia_natural.reservations.domain.exception;

public class RefundActionNotAllowedException extends RuntimeException {

    public RefundActionNotAllowedException(String message) {
        super(message);
    }
}
