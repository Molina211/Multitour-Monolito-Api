package com.corhuila.errorcapa8.travesia_natural.cash.application;

import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.Reservation;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.out.ReservationRepositoryPort;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Objects;

/**
 * Devoluciones ejecutadas en un {@code businessDate} (spec 013): reservas del tenant con
 * {@code refundedAt} no nulo cuya fecha, truncada a UTC, coincide con la fecha buscada. No
 * son un {@code CashMovement} persistido — se calculan en vivo tanto al cerrar una caja
 * como al consultarla mientras está `ABIERTA`.
 */
@Component
public class RefundsTotalCalculator {

    private final ReservationRepositoryPort reservationRepositoryPort;

    public RefundsTotalCalculator(ReservationRepositoryPort reservationRepositoryPort) {
        this.reservationRepositoryPort = reservationRepositoryPort;
    }

    public BigDecimal totalForBusinessDate(String tenantId, LocalDate businessDate) {
        return reservationRepositoryPort.findAllByTenantId(tenantId).stream()
                .filter(reservation -> reservation.refundedAt() != null)
                .filter(reservation -> reservation.refundedAt().atZone(ZoneOffset.UTC).toLocalDate()
                        .equals(businessDate))
                .map(Reservation::refundedAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Igual que {@link #totalForBusinessDate}, agregado por mes (consolidación, RF-012). */
    public BigDecimal totalForPeriod(String tenantId, YearMonth period) {
        return reservationRepositoryPort.findAllByTenantId(tenantId).stream()
                .filter(reservation -> reservation.refundedAt() != null)
                .filter(reservation -> YearMonth.from(reservation.refundedAt().atZone(ZoneOffset.UTC).toLocalDate())
                        .equals(period))
                .map(Reservation::refundedAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
