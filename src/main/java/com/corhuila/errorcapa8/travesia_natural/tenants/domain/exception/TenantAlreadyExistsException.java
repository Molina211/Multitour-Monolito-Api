package com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception;

public class TenantAlreadyExistsException extends RuntimeException {

    public TenantAlreadyExistsException(String tenantId) {
        super("tenant already exists: " + tenantId);
    }
}
