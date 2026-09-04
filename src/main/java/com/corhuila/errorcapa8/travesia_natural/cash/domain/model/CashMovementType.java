package com.corhuila.errorcapa8.travesia_natural.cash.domain.model;

/**
 * Tipos registrables a mano en el endpoint genérico de movimientos (spec 013). No incluye
 * {@code DEVOLUCION}: esa se calcula en vivo desde las devoluciones ejecutadas de
 * {@code reservations} (spec 012), nunca se registra manualmente.
 */
public enum CashMovementType {
    INGRESO,
    PAGO,
    GASTO
}
