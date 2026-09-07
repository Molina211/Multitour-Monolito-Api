package com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception;

public class CollaboratorNotFoundException extends RuntimeException {

    public CollaboratorNotFoundException(String tenantId, String membershipId) {
        super("collaborator not found: " + membershipId + " in tenant " + tenantId);
    }
}
