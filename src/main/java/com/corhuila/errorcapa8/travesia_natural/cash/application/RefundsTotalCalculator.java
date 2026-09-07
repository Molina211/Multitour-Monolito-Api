package com.corhuila.errorcapa8.travesia_natural.cash.application;

import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.Reservation;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.out.ReservationRepositoryPort;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
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

    /**
     * Igual que {@link #totalForBusinessDate}, agregado por mes (consolidación, RF-012).
     * Recibe las reservas ya cargadas en vez de {@code tenantId} porque el llamador
     * ({@code MonthlyCashConsolidationService}) también las necesita para calcular
     * cancelaciones del período: evita consultarlas dos veces en la misma petición.
     */
    public BigDecimal totalForPeriod(List<Reservation> reservations, YearMonth period) {
        return reservations.stream()
                .filter(reservation -> reservation.refundedAt() != null)
                .filter(reservation -> YearMonth.from(reservation.refundedAt().atZone(ZoneOffset.UTC).toLocalDate())
                        .equals(period))
                .map(Reservation::refundedAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
