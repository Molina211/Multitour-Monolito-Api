package com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.in;

public record LoginCommand(String tenantId, String email, String password) {
}
