package com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.in;

public record RegisterCustomerCommand(String tenantId, String firstName, String lastName, String email,
                                       String phone, String password) {
}
