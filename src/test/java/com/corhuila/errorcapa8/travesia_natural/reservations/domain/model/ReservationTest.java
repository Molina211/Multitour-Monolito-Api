package com.corhuila.errorcapa8.travesia_natural.reservations.domain.model;

import com.corhuila.errorcapa8.travesia_natural.reservations.domain.exception.InvalidReservationException;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.exception.PaymentAlreadyResolvedException;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.exception.RefundNotAuthorizedException;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.exception.ReservationNotCancellableException;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.exception.ReservationNotDiscountableException;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.exception.ReservationNotExecutableException;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.exception.ReservationNotFinalizableException;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.exception.ReservationNotModifiableException;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.exception.ReservationNotRefundableException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservationTest {

    private static final String TENANT_ID = "travesia-natural";
    private static final String CUSTOMER_ID = "customer-1";

    private static ReservedService aService() {
        return new ReservedService("tour-laguna-verde", 2, LocalDate.of(2026, 12, 1), null, null);
    }

    private static Reservation aConfirmedReservation() {
        Reservation reservation = Reservation.create(
                UUID.randomUUID(), TENANT_ID, CUSTOMER_ID, BigDecimal.valueOf(100),
                List.of(aService()), "111", List.of());
        return reservation.registerCashPayment(BigDecimal.valueOf(100));
    }

    // --- create ---

    @Test
    void createsAPendingReservationWithFullBalanceDue() {
        UUID reservationId = UUID.randomUUID();
        Reservation reservation = Reservation.create(
                reservationId, TENANT_ID, CUSTOMER_ID, BigDecimal.valueOf(100),
                List.of(aService()), "111", List.of());

        assertThat(reservation.reservationId()).isEqualTo(reservationId);
        assertThat(reservation.tenantId()).isEqualTo(TENANT_ID);
        assertThat(reservation.customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(reservation.reservationStatus()).isEqualTo(ReservationStatus.PENDIENTE_DE_PAGO);
        assertThat(reservation.paymentStatus()).isEqualTo(PaymentStatus.SIN_PAGO);
        assertThat(reservation.projectedValue()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(reservation.finalValue()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(reservation.pendingBalance()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(reservation.creditBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(reservation.companions()).isEmpty();
    }

    @Test
    void rejectsBlankTenantId() {
        assertThatThrownBy(() -> Reservation.create(
                UUID.randomUUID(), " ", CUSTOMER_ID, BigDecimal.TEN, List.of(aService()), "111", List.of()))
                .isInstanceOf(InvalidReservationException.class)
                .hasMessageContaining("tenantId is required");
    }

    @Test
    void rejectsBlankCustomerId() {
        assertThatThrownBy(() -> Reservation.create(
                UUID.randomUUID(), TENANT_ID, " ", BigDecimal.TEN, List.of(aService()), "111", List.of()))
                .isInstanceOf(InvalidReservationException.class)
                .hasMessageContaining("customerId is required");
    }

    @Test
    void rejectsEmptyReservedServices() {
        assertThatThrownBy(() -> Reservation.create(
                UUID.randomUUID(), TENANT_ID, CUSTOMER_ID, BigDecimal.TEN, List.of(), "111", List.of()))
                .isInstanceOf(InvalidReservationException.class)
                .hasMessageContaining("at least one reserved service is required");
    }

    @Test
    void rejectsNegativeProjectedValue() {
        assertThatThrownBy(() -> Reservation.create(
                UUID.randomUUID(), TENANT_ID, CUSTOMER_ID, BigDecimal.valueOf(-1), List.of(aService()), "111",
                List.of()))
                .isInstanceOf(InvalidReservationException.class)
                .hasMessageContaining("projectedValue must be a non-negative amount");
    }

    @Test
    void rejectsDuplicateDocumentBetweenHolderAndCompanion() {
        Companion companion = new Companion("Jane Doe", "111", LocalDate.of(1990, 1, 1));

        assertThatThrownBy(() -> Reservation.create(
                UUID.randomUUID(), TENANT_ID, CUSTOMER_ID, BigDecimal.TEN, List.of(aService()), "111",
                List.of(companion)))
                .isInstanceOf(InvalidReservationException.class)
                .hasMessageContaining("RN-RES-005");
    }

    @Test
    void treatsDifferentlyFormattedEquivalentDocumentsAsDuplicates() {
        Companion companion = new Companion("Jane Doe", "  1-11 ", LocalDate.of(1990, 1, 1));

        assertThatThrownBy(() -> Reservation.create(
                UUID.randomUUID(), TENANT_ID, CUSTOMER_ID, BigDecimal.TEN, List.of(aService()), "111",
                List.of(companion)))
                .isInstanceOf(InvalidReservationException.class)
                .hasMessageContaining("RN-RES-005");
    }

    @Test
    void allowsDistinctDocumentsAmongHolderAndCompanions() {
        Companion companion = new Companion("Jane Doe", "222", LocalDate.of(1990, 1, 1));

        Reservation reservation = Reservation.create(
                UUID.randomUUID(), TENANT_ID, CUSTOMER_ID, BigDecimal.TEN, List.of(aService()), "111",
                List.of(companion));

        assertThat(reservation.companions()).containsExactly(companion);
    }

    // --- registerCashPayment ---

    @Test
    void cashPaymentCoveringFullBalanceConfirmsReservation() {
        Reservation reservation = Reservation.create(
                UUID.randomUUID(), TENANT_ID, CUSTOMER_ID, BigDecimal.valueOf(100), List.of(aService()), "111",
                List.of());

        Reservation paid = reservation.registerCashPayment(BigDecimal.valueOf(100));

        assertThat(paid.reservationStatus()).isEqualTo(ReservationStatus.CONFIRMADA);
        assertThat(paid.paymentStatus()).isEqualTo(PaymentStatus.PAGADO);
        assertThat(paid.paymentMethod()).isEqualTo("Efectivo");
        assertThat(paid.pendingBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void cashPaymentBelowPendingBalanceIsRejected() {
        Reservation reservation = Reservation.create(
                UUID.randomUUID(), TENANT_ID, CUSTOMER_ID, BigDecimal.valueOf(100), List.of(aService()), "111",
                List.of());

        assertThatThrownBy(() -> reservation.registerCashPayment(BigDecimal.valueOf(50)))
                .isInstanceOf(InvalidReservationException.class)
                .hasMessageContaining("cash payment must cover the full pending balance");
    }

    @Test
    void cashPaymentRejectsNonPositiveAmount() {
        Reservation reservation = Reservation.create(
                UUID.randomUUID(), TENANT_ID, CUSTOMER_ID, BigDecimal.valueOf(100), List.of(aService()), "111",
                List.of());

        assertThatThrownBy(() -> reservation.registerCashPayment(BigDecimal.ZERO))
                .isInstanceOf(InvalidReservationException.class)
                .hasMessageContaining("payment amount must be a positive value");
    }

    // --- registerInstallmentPayment ---

    @Test
    void partialInstallmentKeepsReservationPendingWithReducedBalance() {
        Reservation reservation = Reservation.create(
                UUID.randomUUID(), TENANT_ID, CUSTOMER_ID, BigDecimal.valueOf(100), List.of(aService()), "111",
                List.of());

        Reservation afterInstallment = reservation.registerInstallmentPayment(BigDecimal.valueOf(40));

        assertThat(afterInstallment.reservationStatus()).isEqualTo(ReservationStatus.PENDIENTE_DE_PAGO);
        assertThat(afterInstallment.paymentStatus()).isEqualTo(PaymentStatus.PARCIAL);
        assertThat(afterInstallment.pendingBalance()).isEqualByComparingTo(BigDecimal.valueOf(60));
        assertThat(afterInstallment.paymentMethod()).isEqualTo("Abono");
    }

    @Test
    void installmentThatSettlesBalanceConfirmsReservation() {
        Reservation reservation = Reservation.create(
                UUID.randomUUID(), TENANT_ID, CUSTOMER_ID, BigDecimal.valueOf(100), List.of(aService()), "111",
                List.of());

        Reservation settled = reservation.registerInstallmentPayment(BigDecimal.valueOf(100));

        assertThat(settled.reservationStatus()).isEqualTo(ReservationStatus.CONFIRMADA);
        assertThat(settled.paymentStatus()).isEqualTo(PaymentStatus.PAGADO);
        assertThat(settled.pendingBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void installmentGreaterThanPendingBalanceClampsToZero() {
        Reservation reservation = Reservation.create(
                UUID.randomUUID(), TENANT_ID, CUSTOMER_ID, BigDecimal.valueOf(100), List.of(aService()), "111",
                List.of());

        Reservation settled = reservation.registerInstallmentPayment(BigDecimal.valueOf(150));

        assertThat(settled.pendingBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(settled.paymentStatus()).isEqualTo(PaymentStatus.PAGADO);
    }

    // --- transfer payment: register / approve / reject ---

    @Test
    void transferPaymentLeavesBalanceUntouchedAwaitingDecision() {
        Reservation reservation = Reservation.create(
                UUID.randomUUID(), TENANT_ID, CUSTOMER_ID, BigDecimal.valueOf(100), List.of(aService()), "111",
                List.of());

        Reservation withTransfer = reservation.registerTransferPayment(BigDecimal.valueOf(100), "support-ref-1");

        assertThat(withTransfer.paymentStatus()).isEqualTo(PaymentStatus.EN_VALIDACION);
        assertThat(withTransfer.paymentMethod()).isEqualTo("Transferencia");
        assertThat(withTransfer.pendingTransferAmount()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(withTransfer.transferSupportReference()).isEqualTo("support-ref-1");
        assertThat(withTransfer.pendingBalance()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    @Test
    void transferPaymentRequiresSupportReference() {
        Reservation reservation = Reservation.create(
                UUID.randomUUID(), TENANT_ID, CUSTOMER_ID, BigDecimal.valueOf(100), List.of(aService()), "111",
                List.of());

        assertThatThrownBy(() -> reservation.registerTransferPayment(BigDecimal.valueOf(100), " "))
                .isInstanceOf(InvalidReservationException.class)
                .hasMessageContaining("supportReference is required");
    }

    @Test
    void secondTransferWhileOneIsPendingIsRejected() {
        Reservation reservation = Reservation.create(
                        UUID.randomUUID(), TENANT_ID, CUSTOMER_ID, BigDecimal.valueOf(100), List.of(aService()), "111",
                        List.of())
                .registerTransferPayment(BigDecimal.valueOf(50), "support-ref-1");

        assertThatThrownBy(() -> reservation.registerTransferPayment(BigDecimal.valueOf(50), "support-ref-2"))
                .isInstanceOf(PaymentAlreadyResolvedException.class);
    }

    @Test
    void approvingTransferAppliesItAsAnInstallment() {
        Reservation reservation = Reservation.create(
                        UUID.randomUUID(), TENANT_ID, CUSTOMER_ID, BigDecimal.valueOf(100), List.of(aService()), "111",
                        List.of())
                .registerTransferPayment(BigDecimal.valueOf(100), "support-ref-1");

        Reservation approved = reservation.approveTransferPayment();

        assertThat(approved.reservationStatus()).isEqualTo(ReservationStatus.CONFIRMADA);
        assertThat(approved.paymentStatus()).isEqualTo(PaymentStatus.PAGADO);
        assertThat(approved.pendingBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void approvingWithoutAPendingTransferIsRejected() {
        Reservation reservation = Reservation.create(
                UUID.randomUUID(), TENANT_ID, CUSTOMER_ID, BigDecimal.valueOf(100), List.of(aService()), "111",
                List.of());

        assertThatThrownBy(reservation::approveTransferPayment)
                .isInstanceOf(PaymentAlreadyResolvedException.class);
    }

    @Test
    void rejectingTransferMarksPaymentAsRechazadoWithoutTouchingBalance() {
        Reservation reservation = Reservation.create(
                        UUID.randomUUID(), TENANT_ID, CUSTOMER_ID, BigDecimal.valueOf(100), List.of(aService()), "111",
                        List.of())
                .registerTransferPayment(BigDecimal.valueOf(100), "support-ref-1");

        Reservation rejected = reservation.rejectTransferPayment();

        assertThat(rejected.paymentStatus()).isEqualTo(PaymentStatus.RECHAZADO);
        assertThat(rejected.pendingBalance()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    @Test
    void rejectingWithoutAPendingTransferIsRejected() {
        Reservation reservation = Reservation.create(
                UUID.randomUUID(), TENANT_ID, CUSTOMER_ID, BigDecimal.valueOf(100), List.of(aService()), "111",
                List.of());

        assertThatThrownBy(reservation::rejectTransferPayment)
                .isInstanceOf(PaymentAlreadyResolvedException.class);
    }

    // --- startExecution ---

    @Test
    void startExecutionFromConfirmadaMovesToEnEjecucion() {
        Reservation confirmed = aConfirmedReservation();

        Reservation started = confirmed.startExecution();

        assertThat(started.reservationStatus()).isEqualTo(ReservationStatus.EN_EJECUCION);
    }

    @Test
    void startExecutionFromNonConfirmadaIsRejected() {
        Reservation pending = Reservation.create(
                UUID.randomUUID(), TENANT_ID, CUSTOMER_ID, BigDecimal.valueOf(100), List.of(aService()), "111",
                List.of());

        assertThatThrownBy(pending::startExecution)
                .isInstanceOf(ReservationNotExecutableException.class);
    }

    // --- modify ---

    @Test
    void modifyUpdatesServicesAndValuesKeepingStatus() {
        Reservation reservation = Reservation.create(
                UUID.randomUUID(), TENANT_ID, CUSTOMER_ID, BigDecimal.valueOf(100), List.of(aService()), "111",
                List.of());
        ReservedService newService = new ReservedService("tour-nuevo", 3, LocalDate.of(2027, 1, 1), null, null);

        Reservation modified = reservation.modify(
                List.of(newService), BigDecimal.valueOf(120), BigDecimal.valueOf(120), "cliente pidio cambio",
                "actor-1");

        assertThat(modified.reservedServices()).containsExactly(newService);
        assertThat(modified.projectedValue()).isEqualByComparingTo(BigDecimal.valueOf(120));
        assertThat(modified.finalValue()).isEqualByComparingTo(BigDecimal.valueOf(120));
        assertThat(modified.pendingBalance()).isEqualByComparingTo(BigDecimal.valueOf(120));
        assertThat(modified.modificationReason()).isEqualTo("cliente pidio cambio");
        assertThat(modified.modifiedBy()).isEqualTo("actor-1");
    }

    @Test
    void modifyToLowerValueThanAlreadyPaidGeneratesCreditBalanceAndRefundRequest() {
        Reservation reservation = aConfirmedReservation();

        Reservation modified = reservation.modify(
                List.of(aService()), BigDecimal.valueOf(60), BigDecimal.valueOf(60), "reduccion de alcance",
                "actor-1");

        assertThat(modified.pendingBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(modified.creditBalance()).isEqualByComparingTo(BigDecimal.valueOf(40));
        assertThat(modified.paymentStatus()).isEqualTo(PaymentStatus.SALDO_A_FAVOR_PENDIENTE);
        assertThat(modified.refundDecisionStatus()).isEqualTo(RefundDecisionStatus.PENDIENTE_AUTORIZACION);
    }

    @Test
    void modifyRequiresNonBlankReason() {
        Reservation reservation = Reservation.create(
                UUID.randomUUID(), TENANT_ID, CUSTOMER_ID, BigDecimal.valueOf(100), List.of(aService()), "111",
                List.of());

        assertThatThrownBy(() -> reservation.modify(List.of(aService()), BigDecimal.TEN, BigDecimal.TEN, " ",
                "actor-1"))
                .isInstanceOf(InvalidReservationException.class)
                .hasMessageContaining("modification reason is required");
    }

    @Test
    void modifyRejectsWhenStatusIsNotPendingOrConfirmed() {
        Reservation cancelled = Reservation.create(
                        UUID.randomUUID(), TENANT_ID, CUSTOMER_ID, BigDecimal.valueOf(100), List.of(aService()), "111",
                        List.of())
                .cancel("cliente desistio", "actor-1");

        assertThatThrownBy(() -> cancelled.modify(List.of(aService()), BigDecimal.TEN, BigDecimal.TEN, "reason",
                "actor-1"))
                .isInstanceOf(ReservationNotModifiableException.class);
    }

    @Test
    void modifyRejectsWhenTransferIsAwaitingDecision() {
        Reservation withTransfer = Reservation.create(
                        UUID.randomUUID(), TENANT_ID, CUSTOMER_ID, BigDecimal.valueOf(100), List.of(aService()), "111",
                        List.of())
                .registerTransferPayment(BigDecimal.valueOf(100), "support-ref-1");

        assertThatThrownBy(() -> withTransfer.modify(List.of(aService()), BigDecimal.TEN, BigDecimal.TEN, "reason",
                "actor-1"))
                .isInstanceOf(ReservationNotModifiableException.class);
    }

    // --- applyDiscount ---

    @Test
    void applyDiscountReducesFinalValueAndRecalculatesPendingBalance() {
        Reservation reservation = Reservation.create(
                UUID.randomUUID(), TENANT_ID, CUSTOMER_ID, BigDecimal.valueOf(100), List.of(aService()), "111",
                List.of());

        Reservation discounted = reservation.applyDiscount(10, "promocion", "actor-1");

        assertThat(discounted.finalValue()).isEqualByComparingTo(BigDecimal.valueOf(90));
        assertThat(discounted.pendingBalance()).isEqualByComparingTo(BigDecimal.valueOf(90));
        assertThat(discounted.projectedValue()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    @Test
    void applyDiscountBelowAlreadyPaidAmountGeneratesCreditBalance() {
        Reservation reservation = aConfirmedReservation();

        Reservation discounted = reservation.applyDiscount(50, "promocion", "actor-1");

        assertThat(discounted.finalValue()).isEqualByComparingTo(BigDecimal.valueOf(50));
        assertThat(discounted.pendingBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(discounted.creditBalance()).isEqualByComparingTo(BigDecimal.valueOf(50));
        assertThat(discounted.paymentStatus()).isEqualTo(PaymentStatus.SALDO_A_FAVOR_PENDIENTE);
        assertThat(discounted.refundDecisionStatus()).isEqualTo(RefundDecisionStatus.PENDIENTE_AUTORIZACION);
    }

    @Test
    void applyDiscountRejectsPercentageOutOfRange() {
        Reservation reservation = Reservation.create(
                UUID.randomUUID(), TENANT_ID, CUSTOMER_ID, BigDecimal.valueOf(100), List.of(aService()), "111",
                List.of());

        assertThatThrownBy(() -> reservation.applyDiscount(0, "promocion", "actor-1"))
                .isInstanceOf(InvalidReservationException.class)
                .hasMessageContaining("discount percentage must be between 1 and 100");
        assertThatThrownBy(() -> reservation.applyDiscount(101, "promocion", "actor-1"))
                .isInstanceOf(InvalidReservationException.class);
    }

    @Test
    void applyDiscountRejectsWhenStatusIsNotPendingOrConfirmed() {
        Reservation cancelled = Reservation.create(
                        UUID.randomUUID(), TENANT_ID, CUSTOMER_ID, BigDecimal.valueOf(100), List.of(aService()), "111",
                        List.of())
                .cancel("cliente desistio", "actor-1");

        assertThatThrownBy(() -> cancelled.applyDiscount(10, "promocion", "actor-1"))
                .isInstanceOf(ReservationNotDiscountableException.class);
    }

    // --- cancel ---

    @Test
    void cancelWithoutPriorPaymentDoesNotGenerateCreditBalance() {
        Reservation reservation = Reservation.create(
                UUID.randomUUID(), TENANT_ID, CUSTOMER_ID, BigDecimal.valueOf(100), List.of(aService()), "111",
                List.of());

        Reservation cancelled = reservation.cancel("cliente desistio", "actor-1");

        assertThat(cancelled.reservationStatus()).isEqualTo(ReservationStatus.CANCELADA);
        assertThat(cancelled.creditBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(cancelled.refundDecisionStatus()).isNull();
        assertThat(cancelled.cancellationReason()).isEqualTo("cliente desistio");
        assertThat(cancelled.cancelledBy()).isEqualTo("actor-1");
    }

    @Test
    void cancelWithPriorPaymentGeneratesCreditBalanceAndRefundRequest() {
        Reservation reservation = aConfirmedReservation();

        Reservation cancelled = reservation.cancel("cliente desistio", "actor-1");

        assertThat(cancelled.reservationStatus()).isEqualTo(ReservationStatus.CANCELADA);
        assertThat(cancelled.creditBalance()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(cancelled.paymentStatus()).isEqualTo(PaymentStatus.SALDO_A_FAVOR_PENDIENTE);
        assertThat(cancelled.refundDecisionStatus()).isEqualTo(RefundDecisionStatus.PENDIENTE_AUTORIZACION);
    }

    @Test
    void cancelRequiresNonBlankReason() {
        Reservation reservation = Reservation.create(
                UUID.randomUUID(), TENANT_ID, CUSTOMER_ID, BigDecimal.valueOf(100), List.of(aService()), "111",
                List.of());

        assertThatThrownBy(() -> reservation.cancel(" ", "actor-1"))
                .isInstanceOf(InvalidReservationException.class)
                .hasMessageContaining("cancellation reason is required");
    }

    @Test
    void cancelRejectsWhenAlreadyExecuting() {
        Reservation executing = aConfirmedReservation().startExecution();

        assertThatThrownBy(() -> executing.cancel("cliente desistio", "actor-1"))
                .isInstanceOf(ReservationNotCancellableException.class);
    }

    @Test
    void cancelRejectsWhenTransferIsAwaitingDecision() {
        Reservation withTransfer = Reservation.create(
                        UUID.randomUUID(), TENANT_ID, CUSTOMER_ID, BigDecimal.valueOf(100), List.of(aService()), "111",
                        List.of())
                .registerTransferPayment(BigDecimal.valueOf(100), "support-ref-1");

        assertThatThrownBy(() -> withTransfer.cancel("cliente desistio", "actor-1"))
                .isInstanceOf(ReservationNotCancellableException.class);
    }

    // --- refund workflow: authorize / reject / execute / register as credit balance ---

    private static Reservation aCancelledReservationWithCreditBalance() {
        return aConfirmedReservation().cancel("cliente desistio", "actor-1");
    }

    @Test
    void authorizeRefundMovesToAutorizada() {
        Reservation cancelled = aCancelledReservationWithCreditBalance();

        Reservation authorized = cancelled.authorizeRefund("admin-1", "procede devolucion");

        assertThat(authorized.refundDecisionStatus()).isEqualTo(RefundDecisionStatus.AUTORIZADA);
        assertThat(authorized.refundAuthorizedBy()).isEqualTo("admin-1");
        assertThat(authorized.refundAuthorizationNote()).isEqualTo("procede devolucion");
    }

    @Test
    void authorizeRefundRejectsWhenNotPendingAuthorization() {
        Reservation reservation = Reservation.create(
                UUID.randomUUID(), TENANT_ID, CUSTOMER_ID, BigDecimal.valueOf(100), List.of(aService()), "111",
                List.of());

        assertThatThrownBy(() -> reservation.authorizeRefund("admin-1", "nota"))
                .isInstanceOf(RefundNotAuthorizedException.class);
    }

    @Test
    void rejectRefundMovesToRechazadaWithoutTouchingCreditBalance() {
        Reservation cancelled = aCancelledReservationWithCreditBalance();

        Reservation rejected = cancelled.rejectRefund("admin-1", "no procede");

        assertThat(rejected.refundDecisionStatus()).isEqualTo(RefundDecisionStatus.RECHAZADA);
        assertThat(rejected.refundRejectedBy()).isEqualTo("admin-1");
        assertThat(rejected.refundRejectionReason()).isEqualTo("no procede");
        assertThat(rejected.creditBalance()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(rejected.paymentStatus()).isEqualTo(PaymentStatus.SALDO_A_FAVOR_PENDIENTE);
    }

    @Test
    void refundExecutesOnlyWhenAuthorized() {
        Reservation cancelled = aCancelledReservationWithCreditBalance();

        assertThatThrownBy(() -> cancelled.refund(BigDecimal.valueOf(100), "reembolso", "admin-1", "Transferencia"))
                .isInstanceOf(RefundNotAuthorizedException.class);
    }

    @Test
    void refundReducesCreditBalanceAndMarksAsExecuted() {
        Reservation authorized = aCancelledReservationWithCreditBalance()
                .authorizeRefund("admin-1", "procede devolucion");

        Reservation refunded = authorized.refund(BigDecimal.valueOf(100), "reembolso total", "admin-1",
                "Transferencia");

        assertThat(refunded.creditBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(refunded.paymentStatus()).isEqualTo(PaymentStatus.DEVUELTO_PARCIAL_O_TOTAL);
        assertThat(refunded.refundDecisionStatus()).isEqualTo(RefundDecisionStatus.EJECUTADA);
        assertThat(refunded.refundedAmount()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(refunded.refundReason()).isEqualTo("reembolso total");
        assertThat(refunded.refundedBy()).isEqualTo("admin-1");
        assertThat(refunded.refundMethod()).isEqualTo("Transferencia");
    }

    @Test
    void refundRejectsAmountAboveAvailableCreditBalance() {
        Reservation authorized = aCancelledReservationWithCreditBalance()
                .authorizeRefund("admin-1", "procede devolucion");

        assertThatThrownBy(() -> authorized.refund(BigDecimal.valueOf(200), "reembolso", "admin-1",
                "Transferencia"))
                .isInstanceOf(InvalidReservationException.class)
                .hasMessageContaining("cannot exceed the available creditBalance");
    }

    @Test
    void refundRejectsNonPositiveAmount() {
        Reservation authorized = aCancelledReservationWithCreditBalance()
                .authorizeRefund("admin-1", "procede devolucion");

        assertThatThrownBy(() -> authorized.refund(BigDecimal.ZERO, "reembolso", "admin-1", "Transferencia"))
                .isInstanceOf(InvalidReservationException.class)
                .hasMessageContaining("refund amount must be a positive value");
    }

    @Test
    void registerRefundAsCreditBalanceRequiresAuthorization() {
        Reservation cancelled = aCancelledReservationWithCreditBalance();

        assertThatThrownBy(() -> cancelled.registerRefundAsCreditBalance("admin-1"))
                .isInstanceOf(RefundNotAuthorizedException.class);
    }

    @Test
    void registerRefundAsCreditBalanceMarksDecisionWithoutTouchingCreditBalance() {
        Reservation authorized = aCancelledReservationWithCreditBalance()
                .authorizeRefund("admin-1", "procede devolucion");

        Reservation registered = authorized.registerRefundAsCreditBalance("admin-1");

        assertThat(registered.refundDecisionStatus()).isEqualTo(RefundDecisionStatus.SALDO_A_FAVOR_REGISTRADO);
        assertThat(registered.creditBalance()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(registered.paymentStatus()).isEqualTo(PaymentStatus.SALDO_A_FAVOR_PENDIENTE);
    }

    // --- finalizeExecution ---

    @Test
    void finalizeExecutionFromEnEjecucionMovesToFinalizada() {
        Reservation executing = aConfirmedReservation().startExecution();

        Reservation finalized = executing.finalizeExecution("operador-1");

        assertThat(finalized.reservationStatus()).isEqualTo(ReservationStatus.FINALIZADA);
        assertThat(finalized.finalizedBy()).isEqualTo("operador-1");
        assertThat(finalized.finalizedAt()).isNotNull();
    }

    @Test
    void finalizeExecutionRejectsWhenNotExecuting() {
        Reservation confirmed = aConfirmedReservation();

        assertThatThrownBy(() -> confirmed.finalizeExecution("operador-1"))
                .isInstanceOf(ReservationNotFinalizableException.class);
    }

    // --- reconstitute ---

    @Test
    void reconstituteDoesNotReValidateInvariants() {
        UUID reservationId = UUID.randomUUID();
        Reservation reservation = Reservation.reconstitute(
                reservationId, TENANT_ID, CUSTOMER_ID, List.of(aService()), BigDecimal.TEN, BigDecimal.TEN,
                BigDecimal.TEN, BigDecimal.ZERO, ReservationStatus.PENDIENTE_DE_PAGO, PaymentStatus.SIN_PAGO,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, "111", List.of());

        assertThat(reservation.reservationId()).isEqualTo(reservationId);
        assertThat(reservation.reservationStatus()).isEqualTo(ReservationStatus.PENDIENTE_DE_PAGO);
    }
}
