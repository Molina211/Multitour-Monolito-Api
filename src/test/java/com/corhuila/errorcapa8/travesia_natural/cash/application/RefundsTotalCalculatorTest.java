package com.corhuila.errorcapa8.travesia_natural.cash.application;

import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.PaymentStatus;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.Reservation;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.ReservationStatus;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.out.ReservationRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefundsTotalCalculatorTest {

    private static final String TENANT_ID = "travesia-natural";

    @Mock
    private ReservationRepositoryPort reservationRepositoryPort;

    private RefundsTotalCalculator refundsTotalCalculator;

    @BeforeEach
    void setUp() {
        refundsTotalCalculator = new RefundsTotalCalculator(reservationRepositoryPort);
    }

    private Reservation aRefundedReservation(Instant refundedAt, BigDecimal refundedAmount) {
        return Reservation.reconstitute(UUID.randomUUID(), TENANT_ID, "customer-1", List.of(), BigDecimal.TEN,
                BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO, ReservationStatus.CANCELADA,
                PaymentStatus.DEVUELTO_PARCIAL_O_TOTAL, "EFECTIVO", Instant.now(), null, null, "cancelada", "actor-1",
                Instant.now(), null, null, null, null, null, null, null, refundedAmount, "reembolso", "actor-1",
                "EFECTIVO", refundedAt, null, null, null, null, null, "cc-1", List.of());
    }

    private Reservation aReservationWithoutRefund() {
        return aRefundedReservation(null, null);
    }

    @Test
    void sumsRefundsForAMatchingBusinessDate() {
        Instant refundedAt = Instant.parse("2026-09-05T15:00:00Z");
        Reservation refunded = aRefundedReservation(refundedAt, BigDecimal.valueOf(30000));
        Reservation notRefunded = aReservationWithoutRefund();
        when(reservationRepositoryPort.findAllByTenantId(TENANT_ID)).thenReturn(List.of(refunded, notRefunded));

        BigDecimal total = refundsTotalCalculator.totalForBusinessDate(TENANT_ID,
                refundedAt.atZone(ZoneOffset.UTC).toLocalDate());

        assertThat(total).isEqualByComparingTo(BigDecimal.valueOf(30000));
    }

    @Test
    void returnsZeroWhenNoRefundsMatchTheBusinessDate() {
        when(reservationRepositoryPort.findAllByTenantId(TENANT_ID)).thenReturn(List.of(aReservationWithoutRefund()));

        BigDecimal total = refundsTotalCalculator.totalForBusinessDate(TENANT_ID,
                Instant.now().atZone(ZoneOffset.UTC).toLocalDate());

        assertThat(total).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void sumsRefundsForAMatchingPeriod() {
        Instant refundedAt = Instant.parse("2026-09-05T15:00:00Z");
        Reservation refunded = aRefundedReservation(refundedAt, BigDecimal.valueOf(30000));

        BigDecimal total = refundsTotalCalculator.totalForPeriod(List.of(refunded), YearMonth.of(2026, 9));

        assertThat(total).isEqualByComparingTo(BigDecimal.valueOf(30000));
    }
}
