package com.corhuila.errorcapa8.travesia_natural.reservations.domain.exception;

public class RefundNotAuthorizedException extends RuntimeException {

    public RefundNotAuthorizedException(String message) {
        super(message);
    }
}
