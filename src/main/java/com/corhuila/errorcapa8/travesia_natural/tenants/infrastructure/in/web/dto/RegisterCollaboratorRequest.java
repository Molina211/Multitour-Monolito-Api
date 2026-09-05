package com.corhuila.errorcapa8.travesia_natural.tenants.infrastructure.in.web.dto;

public record RegisterCollaboratorRequest(String name, String email, String password, String passwordConfirmation,
                                           String actorId) {
}
