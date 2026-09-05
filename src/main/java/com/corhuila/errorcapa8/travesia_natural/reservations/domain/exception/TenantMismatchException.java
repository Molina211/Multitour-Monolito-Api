package com.corhuila.errorcapa8.travesia_natural.reservations.domain.exception;

public class TenantMismatchException extends RuntimeException {

    public TenantMismatchException() {
        super("authenticated token does not belong to this tenant");
    }
}
