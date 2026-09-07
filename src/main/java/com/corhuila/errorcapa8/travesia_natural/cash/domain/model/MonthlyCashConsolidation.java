package com.corhuila.errorcapa8.travesia_natural.cash.domain.model;

import java.math.BigDecimal;

/**
 * Consolidación mensual (RF-012, spec 013): agrega los cierres de caja de un periodo más
 * dos lecturas cruzadas de solo lectura (cancelaciones de {@code reservations}, costos
 * operacionales de {@code operations}) — mismos 7 valores que
 * {@code cash-monthly.component.html} en el Frontend real.
 */
public record MonthlyCashConsolidation(String period, BigDecimal ingresos, BigDecimal pagosOperacionales,
                                        BigDecimal gastos, BigDecimal devoluciones, BigDecimal total,
                                        long cancelaciones, BigDecimal costosOperacionales) {
}
