package com.corhuila.errorcapa8.travesia_natural.tenants.infrastructure.in.web.dto;

public record RegisterCustomerRequest(String firstName, String lastName, String email, String phone,
                                       String password, String passwordConfirmation) {
}
