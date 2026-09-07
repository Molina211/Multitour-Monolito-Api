package com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception;

/**
 * The single, generic rejection reason for a login attempt (spec 004, HU-IAM-002
 * escenarios 2 y 3). Deliberately has no subtype and no per-case constructor: whether
 * the tenant does not exist, the tenant is Inactivo, the email has no membership in
 * that tenant, the membership is INACTIVA, or the password does not match, the caller
 * always throws this exact exception with this exact message. Collapsing every case
 * into one type means a future change to LoginService or AuthController cannot
 * accidentally leak which case applied by adding a distinct message or status code for
 * one of them — there is only one to map.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("email o password incorrectos");
    }
}
