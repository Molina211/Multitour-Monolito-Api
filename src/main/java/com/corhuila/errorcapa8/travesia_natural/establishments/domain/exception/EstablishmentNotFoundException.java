package com.corhuila.errorcapa8.travesia_natural.establishments.domain.exception;

public class EstablishmentNotFoundException extends RuntimeException {

    public EstablishmentNotFoundException(String establishmentId) {
        super("establishment not found: " + establishmentId);
    }
}
