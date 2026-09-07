package com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception;

public class TenantInactiveException extends RuntimeException {

    public TenantInactiveException(String tenantId) {
        super("tenant is Inactivo: " + tenantId);
    }
}
