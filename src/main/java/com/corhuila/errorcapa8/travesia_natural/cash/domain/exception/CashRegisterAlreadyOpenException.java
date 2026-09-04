package com.corhuila.errorcapa8.travesia_natural.cash.domain.exception;

public class CashRegisterAlreadyOpenException extends RuntimeException {

    public CashRegisterAlreadyOpenException(String message) {
        super(message);
    }
}
