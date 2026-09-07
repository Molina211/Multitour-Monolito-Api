package com.corhuila.errorcapa8.travesia_natural.tenants.infrastructure.in.web.dto;

import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.in.LoginResult;

public record LoginResponse(String accessToken, String membershipId, String tenantId, String firstName,
                             String lastName, String email, String role) {

    public static LoginResponse from(LoginResult result) {
        return new LoginResponse(
                result.accessToken(),
                result.membership().membershipId().toString(),
                result.membership().tenantId(),
                result.membership().firstName(),
                result.membership().lastName(),
                result.membership().email(),
                result.membership().role().name());
    }
}
