package com.corhuila.errorcapa8.travesia_natural.operations.domain.exception;

public class ExecutionNotFoundException extends RuntimeException {

    public ExecutionNotFoundException(String reservationId) {
        super("execution not found for reservation: " + reservationId);
    }
}
