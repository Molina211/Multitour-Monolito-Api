package com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception;

public class TenantPermissionNotAllowedException extends RuntimeException {

    public TenantPermissionNotAllowedException(String message) {
        super(message);
    }
}
