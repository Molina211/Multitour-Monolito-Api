package com.corhuila.errorcapa8.travesia_natural.cash.infrastructure.in.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * {@code actorId} es parte del contrato (spec 013, AC de apertura) pero no se persiste:
 * el esquema mínimo de {@code cash_registers} no tiene columna {@code opened_by}
 * (a diferencia de {@code closed_by}), decisión explícita de plan.md.
 */
public record OpenCashRegisterRequest(LocalDate businessDate, BigDecimal baseAmount, String actorId) {
}
