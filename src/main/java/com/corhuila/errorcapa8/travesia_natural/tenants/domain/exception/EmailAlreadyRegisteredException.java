package com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception;

public class EmailAlreadyRegisteredException extends RuntimeException {

    public EmailAlreadyRegisteredException(String tenantId, String email) {
        super("email already registered in tenant " + tenantId + ": " + email);
    }
}
