package com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception;

public class TenantNotFoundException extends RuntimeException {

    public TenantNotFoundException(String tenantId) {
        super("tenant not found: " + tenantId);
    }
}
