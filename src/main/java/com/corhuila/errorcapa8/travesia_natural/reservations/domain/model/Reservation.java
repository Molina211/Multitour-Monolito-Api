package com.corhuila.errorcapa8.travesia_natural.reservations.domain.model;

import com.corhuila.errorcapa8.travesia_natural.reservations.domain.exception.InvalidReservationException;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.exception.PaymentAlreadyResolvedException;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.exception.RefundNotAuthorizedException;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.exception.ReservationNotCancellableException;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.exception.ReservationNotExecutableException;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.exception.ReservationNotFinalizableException;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.exception.ReservationNotRefundableException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Aggregate root for the Reservations bounded context.
 * Scope of this cut: creation only (02-domain/entities-and-rules.md, "Aggregate: Reservation").
 * Discount calculation, capacity policy and rescheduling are out of scope (spec 001).
 */
public final class Reservation {

    private final UUID reservationId;
    private final String tenantId;
    private final String customerId;
    private final List<ReservedService> reservedServices;
    private final BigDecimal projectedValue;
    private final BigDecimal finalValue;
    private final BigDecimal pendingBalance;
    private final BigDecimal creditBalance;
    private final ReservationStatus reservationStatus;
    private final PaymentStatus paymentStatus;
    private final String paymentMethod;
    private final Instant createdAt;
    private final BigDecimal pendingTransferAmount;
    private final String transferSupportReference;
    private final String cancellationReason;
    private final String cancelledBy;
    private final Instant cancelledAt;
    private final RefundDecisionStatus refundDecisionStatus;
    private final String refundAuthorizedBy;
    private final Instant refundAuthorizedAt;
    private final String refundAuthorizationNote;
    private final String refundRejectedBy;
    private final Instant refundRejectedAt;
    private final String refundRejectionReason;
    private final BigDecimal refundedAmount;
    private final String refundReason;
    private final String refundedBy;
    private final String refundMethod;
    private final Instant refundedAt;
    private final String finalizedBy;
    private final Instant finalizedAt;
    private final String holderDocument;
    private final List<Companion> companions;

    private Reservation(UUID reservationId, String tenantId, String customerId,
                         List<ReservedService> reservedServices, BigDecimal projectedValue,
                         BigDecimal finalValue, BigDecimal pendingBalance, BigDecimal creditBalance,
                         ReservationStatus reservationStatus, PaymentStatus paymentStatus,
                         String paymentMethod, Instant createdAt, BigDecimal pendingTransferAmount,
                         String transferSupportReference, String cancellationReason, String cancelledBy,
                         Instant cancelledAt, RefundDecisionStatus refundDecisionStatus,
                         String refundAuthorizedBy, Instant refundAuthorizedAt, String refundAuthorizationNote,
                         String refundRejectedBy, Instant refundRejectedAt, String refundRejectionReason,
                         BigDecimal refundedAmount, String refundReason, String refundedBy, String refundMethod,
                         Instant refundedAt, String finalizedBy, Instant finalizedAt, String holderDocument,
                         List<Companion> companions) {
        this.reservationId = reservationId;
        this.tenantId = tenantId;
        this.customerId = customerId;
        this.reservedServices = reservedServices;
        this.projectedValue = projectedValue;
        this.finalValue = finalValue;
        this.pendingBalance = pendingBalance;
        this.creditBalance = creditBalance;
        this.reservationStatus = reservationStatus;
        this.paymentStatus = paymentStatus;
        this.paymentMethod = paymentMethod;
        this.createdAt = createdAt;
        this.pendingTransferAmount = pendingTransferAmount;
        this.transferSupportReference = transferSupportReference;
        this.cancellationReason = cancellationReason;
        this.cancelledBy = cancelledBy;
        this.cancelledAt = cancelledAt;
        this.refundDecisionStatus = refundDecisionStatus;
        this.refundAuthorizedBy = refundAuthorizedBy;
        this.refundAuthorizedAt = refundAuthorizedAt;
        this.refundAuthorizationNote = refundAuthorizationNote;
        this.refundRejectedBy = refundRejectedBy;
        this.refundRejectedAt = refundRejectedAt;
        this.refundRejectionReason = refundRejectionReason;
        this.refundedAmount = refundedAmount;
        this.refundReason = refundReason;
        this.refundedBy = refundedBy;
        this.refundMethod = refundMethod;
        this.refundedAt = refundedAt;
        this.finalizedBy = finalizedBy;
        this.finalizedAt = finalizedAt;
        this.holderDocument = holderDocument;
        this.companions = companions;
    }

    /**
     * Creates a new reservation as a commercial projection (HU-RES-001).
     * {@code reservationId} is generated by the caller (application layer, per ADR-002/spec 001
     * decision), not by this factory, so it can be known before persistence.
     */
    public static Reservation create(UUID reservationId, String tenantId, String customerId,
                                      BigDecimal projectedValue, List<ReservedService> reservedServices,
                                      String holderDocument, List<Companion> companions) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new InvalidReservationException("tenantId is required");
        }
        if (customerId == null || customerId.isBlank()) {
            throw new InvalidReservationException("customerId is required");
        }
        if (reservedServices == null || reservedServices.isEmpty()) {
            throw new InvalidReservationException("at least one reserved service is required");
        }
        if (projectedValue == null || projectedValue.signum() < 0) {
            throw new InvalidReservationException("projectedValue must be a non-negative amount");
        }
        List<Companion> safeCompanions = companions == null ? List.of() : List.copyOf(companions);
        requireNoDuplicateDocuments(holderDocument, safeCompanions);

        return new Reservation(
                reservationId,
                tenantId,
                customerId,
                List.copyOf(reservedServices),
                projectedValue,
                projectedValue,
                projectedValue,
                BigDecimal.ZERO,
                ReservationStatus.PENDIENTE_DE_PAGO,
                PaymentStatus.SIN_PAGO,
                null,
                Instant.now(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                holderDocument,
                safeCompanions);
    }

    /**
     * RN-RES-005: el documento del titular no puede repetirse con el de ningún
     * acompañante, ni un acompañante repetir el documento de otro, dentro de la misma
     * reserva. La comparación normaliza (mismo criterio que ya usa el Frontend en
     * {@code normalizeDocument()}: trim, minúsculas, solo alfanumérico) para que
     * variaciones de formato del mismo documento cuenten como duplicado; el valor
     * guardado no se modifica.
     */
    private static void requireNoDuplicateDocuments(String holderDocument, List<Companion> companions) {
        List<String> normalizedDocuments = new ArrayList<>();
        if (holderDocument != null && !holderDocument.isBlank()) {
            normalizedDocuments.add(normalizeDocument(holderDocument));
        }
        for (Companion companion : companions) {
            normalizedDocuments.add(normalizeDocument(companion.document()));
        }
        Set<String> seen = new HashSet<>();
        for (String document : normalizedDocuments) {
            if (!seen.add(document)) {
                throw new InvalidReservationException(
                        "duplicate document within the same reservation is not allowed (RN-RES-005)");
            }
        }
    }

    private static String normalizeDocument(String value) {
        return value.trim().toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    /**
     * Reconstructs a reservation exactly as persisted, without re-validating creation
     * invariants (mismo patrón que {@code CatalogItem.reconstitute(...)}).
     */
    public static Reservation reconstitute(UUID reservationId, String tenantId, String customerId,
                                            List<ReservedService> reservedServices, BigDecimal projectedValue,
                                            BigDecimal finalValue, BigDecimal pendingBalance,
                                            BigDecimal creditBalance, ReservationStatus reservationStatus,
                                            PaymentStatus paymentStatus, String paymentMethod, Instant createdAt,
                                            BigDecimal pendingTransferAmount, String transferSupportReference,
                                            String cancellationReason, String cancelledBy, Instant cancelledAt,
                                            RefundDecisionStatus refundDecisionStatus, String refundAuthorizedBy,
                                            Instant refundAuthorizedAt, String refundAuthorizationNote,
                                            String refundRejectedBy, Instant refundRejectedAt,
                                            String refundRejectionReason, BigDecimal refundedAmount,
                                            String refundReason, String refundedBy, String refundMethod,
                                            Instant refundedAt, String finalizedBy, Instant finalizedAt,
                                            String holderDocument, List<Companion> companions) {
        return new Reservation(reservationId, tenantId, customerId, reservedServices, projectedValue, finalValue,
                pendingBalance, creditBalance, reservationStatus, paymentStatus, paymentMethod, createdAt,
                pendingTransferAmount, transferSupportReference, cancellationReason, cancelledBy, cancelledAt,
                refundDecisionStatus, refundAuthorizedBy, refundAuthorizedAt, refundAuthorizationNote,
                refundRejectedBy, refundRejectedAt, refundRejectionReason, refundedAmount, refundReason, refundedBy,
                refundMethod, refundedAt, finalizedBy, finalizedAt, holderDocument, companions);
    }

    /**
     * Registra un pago en efectivo (spec 009): debe cubrir exactamente o superar el
     * saldo pendiente en una sola operación; un monto insuficiente se rechaza en vez de
     * aceptarse como abono parcial (esa es la modalidad Abono, no Efectivo).
     */
    public Reservation registerCashPayment(BigDecimal amount) {
        validatePaymentAmount(amount);
        if (amount.compareTo(pendingBalance) < 0) {
            throw new InvalidReservationException(
                    "cash payment must cover the full pending balance; use an installment (Abono) for partial payments");
        }
        return new Reservation(reservationId, tenantId, customerId, reservedServices, projectedValue, finalValue,
                BigDecimal.ZERO, creditBalance, ReservationStatus.CONFIRMADA, PaymentStatus.PAGADO, "Efectivo",
                createdAt, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, holderDocument, companions);
    }

    /**
     * Registra un abono (spec 009): puede ser parcial. Si el saldo llega a cero, la
     * reserva queda confirmada igual que un pago en efectivo completo.
     */
    public Reservation registerInstallmentPayment(BigDecimal amount) {
        validatePaymentAmount(amount);
        BigDecimal newPendingBalance = pendingBalance.subtract(amount);
        if (newPendingBalance.signum() < 0) {
            newPendingBalance = BigDecimal.ZERO;
        }
        boolean settled = newPendingBalance.signum() == 0;
        return new Reservation(reservationId, tenantId, customerId, reservedServices, projectedValue, finalValue,
                newPendingBalance, creditBalance,
                settled ? ReservationStatus.CONFIRMADA : reservationStatus,
                settled ? PaymentStatus.PAGADO : PaymentStatus.PARCIAL,
                "Abono", createdAt, null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, holderDocument, companions);
    }

    /**
     * Registra una transferencia con soporte (spec 009): no aplica el monto todavía,
     * queda a la espera de una decisión explícita (aprobar/rechazar).
     */
    public Reservation registerTransferPayment(BigDecimal amount, String supportReference) {
        validatePaymentAmount(amount);
        if (supportReference == null || supportReference.isBlank()) {
            throw new InvalidReservationException("supportReference is required for a transfer payment");
        }
        if (pendingTransferAmount != null) {
            throw new PaymentAlreadyResolvedException(
                    "a transfer is already awaiting a support decision for reservation: " + reservationId);
        }
        return new Reservation(reservationId, tenantId, customerId, reservedServices, projectedValue, finalValue,
                pendingBalance, creditBalance, reservationStatus, PaymentStatus.EN_VALIDACION, "Transferencia",
                createdAt, amount, supportReference, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, holderDocument, companions);
    }

    /**
     * Aprueba la transferencia en espera: aplica su monto exactamente igual que un
     * abono.
     */
    public Reservation approveTransferPayment() {
        if (pendingTransferAmount == null) {
            throw new PaymentAlreadyResolvedException(
                    "no transfer is awaiting a support decision for reservation: " + reservationId);
        }
        return new Reservation(reservationId, tenantId, customerId, reservedServices, projectedValue, finalValue,
                pendingBalance, creditBalance, reservationStatus, paymentStatus, paymentMethod, createdAt,
                pendingTransferAmount, transferSupportReference, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, holderDocument, companions)
                .registerInstallmentPayment(pendingTransferAmount);
    }

    /**
     * Rechaza la transferencia en espera: no modifica el saldo pendiente, solo marca el
     * pago como rechazado.
     */
    public Reservation rejectTransferPayment() {
        if (pendingTransferAmount == null) {
            throw new PaymentAlreadyResolvedException(
                    "no transfer is awaiting a support decision for reservation: " + reservationId);
        }
        return new Reservation(reservationId, tenantId, customerId, reservedServices, projectedValue, finalValue,
                pendingBalance, creditBalance, reservationStatus, PaymentStatus.RECHAZADO, paymentMethod,
                createdAt, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, holderDocument, companions);
    }

    /**
     * Inicia la ejecución de la reserva (spec 010): solo permitido desde `Confirmada`.
     * El mismo guard cubre tanto "nunca se confirmó" como "ya se está ejecutando" (o
     * ya finalizó/canceló), porque tras la primera ejecución el estado deja de ser
     * `Confirmada`.
     */
    public Reservation startExecution() {
        if (reservationStatus != ReservationStatus.CONFIRMADA) {
            throw new ReservationNotExecutableException(
                    "reservation must be Confirmada to start execution, current status: "
                            + reservationStatus.label() + " (reservation: " + reservationId + ")");
        }
        return new Reservation(reservationId, tenantId, customerId, reservedServices, projectedValue, finalValue,
                pendingBalance, creditBalance, ReservationStatus.EN_EJECUCION, paymentStatus, paymentMethod,
                createdAt, pendingTransferAmount, transferSupportReference, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, holderDocument, companions);
    }

    /**
     * Cancela la reserva antes de que inicie ejecución (spec 011): solo permitido desde
     * `PendienteDePago` o `Confirmada`, y solo si no hay una transferencia en espera de
     * aprobación/rechazo (esa decisión debe resolverse primero, spec 009). Si ya se
     * había recibido dinero (`finalValue - pendingBalance > 0`), ese monto queda como
     * saldo a favor pendiente de devolución y la solicitud de devolución nace en
     * `PENDIENTE_AUTORIZACION` (spec 019, RN-RES-008); si no hubo ningún pago, no se
     * genera saldo a favor ni solicitud de devolución.
     */
    public Reservation cancel(String reason, String actorId) {
        if (reason == null || reason.isBlank()) {
            throw new InvalidReservationException("cancellation reason is required");
        }
        if (reservationStatus != ReservationStatus.PENDIENTE_DE_PAGO && reservationStatus != ReservationStatus.CONFIRMADA) {
            throw new ReservationNotCancellableException(
                    "reservation must be PendienteDePago or Confirmada to be cancelled, current status: "
                            + reservationStatus.label() + " (reservation: " + reservationId + ")");
        }
        if (pendingTransferAmount != null) {
            throw new ReservationNotCancellableException(
                    "reservation has a transfer awaiting a support decision, resolve it before cancelling (reservation: "
                            + reservationId + ")");
        }
        BigDecimal amountAlreadyPaid = finalValue.subtract(pendingBalance);
        BigDecimal newCreditBalance = amountAlreadyPaid.signum() > 0 ? amountAlreadyPaid : creditBalance;
        PaymentStatus newPaymentStatus =
                amountAlreadyPaid.signum() > 0 ? PaymentStatus.SALDO_A_FAVOR_PENDIENTE : paymentStatus;
        RefundDecisionStatus newRefundDecisionStatus =
                amountAlreadyPaid.signum() > 0 ? RefundDecisionStatus.PENDIENTE_AUTORIZACION : null;
        return new Reservation(reservationId, tenantId, customerId, reservedServices, projectedValue, finalValue,
                pendingBalance, newCreditBalance, ReservationStatus.CANCELADA, newPaymentStatus, paymentMethod,
                createdAt, pendingTransferAmount, transferSupportReference, reason, actorId, Instant.now(),
                newRefundDecisionStatus, null, null, null, null, null, null,
                null, null, null, null, null, null, null, holderDocument, companions);
    }

    /**
     * Autoriza la solicitud de devolución (spec 019, RN-RES-008): solo permitido desde
     * `PENDIENTE_AUTORIZACION`. La validación de que el actor tenga rol `ADMINISTRATOR`
     * es responsabilidad de la capa de aplicación (necesita resolver el `Membership`),
     * no de este método.
     */
    public Reservation authorizeRefund(String actorId, String note) {
        if (actorId == null || actorId.isBlank()) {
            throw new InvalidReservationException("refund authorization actorId is required");
        }
        if (note == null || note.isBlank()) {
            throw new InvalidReservationException("refund authorization note is required");
        }
        requireRefundDecisionStatus(RefundDecisionStatus.PENDIENTE_AUTORIZACION, "authorize");
        return new Reservation(reservationId, tenantId, customerId, reservedServices, projectedValue, finalValue,
                pendingBalance, creditBalance, reservationStatus, paymentStatus, paymentMethod, createdAt,
                pendingTransferAmount, transferSupportReference, cancellationReason, cancelledBy, cancelledAt,
                RefundDecisionStatus.AUTORIZADA, actorId, Instant.now(), note, null, null, null,
                refundedAmount, refundReason, refundedBy, refundMethod, refundedAt, finalizedBy, finalizedAt,
                holderDocument, companions);
    }

    /**
     * Rechaza la solicitud de devolución (spec 019): solo permitido desde
     * `PENDIENTE_AUTORIZACION`. No modifica `paymentStatus` ni `creditBalance` — el
     * saldo a favor sigue pendiente, solo cambia el estado de la decisión.
     */
    public Reservation rejectRefund(String actorId, String reason) {
        if (actorId == null || actorId.isBlank()) {
            throw new InvalidReservationException("refund rejection actorId is required");
        }
        if (reason == null || reason.isBlank()) {
            throw new InvalidReservationException("refund rejection reason is required");
        }
        requireRefundDecisionStatus(RefundDecisionStatus.PENDIENTE_AUTORIZACION, "reject");
        return new Reservation(reservationId, tenantId, customerId, reservedServices, projectedValue, finalValue,
                pendingBalance, creditBalance, reservationStatus, paymentStatus, paymentMethod, createdAt,
                pendingTransferAmount, transferSupportReference, cancellationReason, cancelledBy, cancelledAt,
                RefundDecisionStatus.RECHAZADA, null, null, null, actorId, Instant.now(), reason,
                refundedAmount, refundReason, refundedBy, refundMethod, refundedAt, finalizedBy, finalizedAt,
                holderDocument, companions);
    }

    /**
     * Ejecuta la devolución del saldo a favor con salida real de dinero (spec 012,
     * ampliado por spec 019/RN-RES-008): exige que la solicitud ya esté `AUTORIZADA` —
     * sin autorización previa trazable no se permite ejecutar. Es un paso único: tras
     * ejecutarse, `refundDecisionStatus` pasa a `EJECUTADA` y no admite una segunda
     * ejecución sobre el mismo saldo, sea total o parcial.
     */
    public Reservation refund(BigDecimal amount, String reason, String actorId, String method) {
        if (reason == null || reason.isBlank()) {
            throw new InvalidReservationException("refund reason is required");
        }
        if (actorId == null || actorId.isBlank()) {
            throw new InvalidReservationException("refund actorId is required");
        }
        if (paymentStatus != PaymentStatus.SALDO_A_FAVOR_PENDIENTE || creditBalance.signum() <= 0) {
            throw new ReservationNotRefundableException(
                    "reservation must have a pending credit balance to be refunded, current paymentStatus: "
                            + paymentStatus.label() + " (reservation: " + reservationId + ")");
        }
        requireRefundDecisionStatus(RefundDecisionStatus.AUTORIZADA, "execute");
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidReservationException("refund amount must be a positive value");
        }
        if (amount.compareTo(creditBalance) > 0) {
            throw new InvalidReservationException(
                    "refund amount cannot exceed the available creditBalance (reservation: " + reservationId + ")");
        }
        BigDecimal newCreditBalance = creditBalance.subtract(amount);
        return new Reservation(reservationId, tenantId, customerId, reservedServices, projectedValue, finalValue,
                pendingBalance, newCreditBalance, reservationStatus, PaymentStatus.DEVUELTO_PARCIAL_O_TOTAL,
                paymentMethod, createdAt, pendingTransferAmount, transferSupportReference, cancellationReason,
                cancelledBy, cancelledAt, RefundDecisionStatus.EJECUTADA, refundAuthorizedBy, refundAuthorizedAt,
                refundAuthorizationNote, refundRejectedBy, refundRejectedAt, refundRejectionReason,
                amount, reason, actorId, method, Instant.now(), finalizedBy, finalizedAt, holderDocument, companions);
    }

    /**
     * Registra la solicitud de devolución `AUTORIZADA` como saldo a favor pendiente sin
     * salida efectiva de dinero (spec 019, RN-RES-008: nunca debe marcarse como
     * `EJECUTADA` si no hay salida real de caja). No modifica `paymentStatus` ni
     * `creditBalance` — el saldo sigue disponible para una ejecución futura o para
     * aplicarse a otra reserva (fuera de alcance de esta spec).
     */
    public Reservation registerRefundAsCreditBalance(String actorId) {
        if (actorId == null || actorId.isBlank()) {
            throw new InvalidReservationException("refund actorId is required");
        }
        requireRefundDecisionStatus(RefundDecisionStatus.AUTORIZADA, "register as credit balance");
        return new Reservation(reservationId, tenantId, customerId, reservedServices, projectedValue, finalValue,
                pendingBalance, creditBalance, reservationStatus, paymentStatus, paymentMethod, createdAt,
                pendingTransferAmount, transferSupportReference, cancellationReason, cancelledBy, cancelledAt,
                RefundDecisionStatus.SALDO_A_FAVOR_REGISTRADO, refundAuthorizedBy, refundAuthorizedAt,
                refundAuthorizationNote, refundRejectedBy, refundRejectedAt, refundRejectionReason,
                refundedAmount, refundReason, refundedBy, refundMethod, refundedAt, finalizedBy, finalizedAt,
                holderDocument, companions);
    }

    private void requireRefundDecisionStatus(RefundDecisionStatus expected, String action) {
        if (refundDecisionStatus != expected) {
            throw new RefundNotAuthorizedException(
                    "cannot " + action + " refund, current refundDecisionStatus: "
                            + (refundDecisionStatus == null ? "none" : refundDecisionStatus.label())
                            + " (reservation: " + reservationId + ")");
        }
    }

    /**
     * Finaliza la ejecución de la reserva (spec 017): solo permitido desde
     * `EnEjecucion` (Sección 16 "Reserva" del PRD: "cuando termina la prestación del
     * servicio y se cierra operativamente"). No modifica precio, descuentos, pago ni
     * saldo — solo cierra la ejecución. El mismo guard cubre tanto "todavía no inició
     * ejecución" como "ya se finalizó", porque tras finalizar el estado deja de ser
     * `EnEjecucion`.
     */
    public Reservation finalizeExecution(String actorId) {
        if (reservationStatus != ReservationStatus.EN_EJECUCION) {
            throw new ReservationNotFinalizableException(
                    "reservation must be En ejecucion to be finalized, current status: "
                            + reservationStatus.label() + " (reservation: " + reservationId + ")");
        }
        return new Reservation(reservationId, tenantId, customerId, reservedServices, projectedValue, finalValue,
                pendingBalance, creditBalance, ReservationStatus.FINALIZADA, paymentStatus, paymentMethod,
                createdAt, pendingTransferAmount, transferSupportReference, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, actorId, Instant.now(), holderDocument,
                companions);
    }

    private void validatePaymentAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidReservationException("payment amount must be a positive value");
        }
    }

    public UUID reservationId() {
        return reservationId;
    }

    public String tenantId() {
        return tenantId;
    }

    public String customerId() {
        return customerId;
    }

    public List<ReservedService> reservedServices() {
        return reservedServices;
    }

    public BigDecimal projectedValue() {
        return projectedValue;
    }

    public BigDecimal finalValue() {
        return finalValue;
    }

    public BigDecimal pendingBalance() {
        return pendingBalance;
    }

    public BigDecimal creditBalance() {
        return creditBalance;
    }

    public ReservationStatus reservationStatus() {
        return reservationStatus;
    }

    public PaymentStatus paymentStatus() {
        return paymentStatus;
    }

    public String paymentMethod() {
        return paymentMethod;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public BigDecimal pendingTransferAmount() {
        return pendingTransferAmount;
    }

    public String transferSupportReference() {
        return transferSupportReference;
    }

    public String cancellationReason() {
        return cancellationReason;
    }

    public String cancelledBy() {
        return cancelledBy;
    }

    public Instant cancelledAt() {
        return cancelledAt;
    }

    public RefundDecisionStatus refundDecisionStatus() {
        return refundDecisionStatus;
    }

    public String refundAuthorizedBy() {
        return refundAuthorizedBy;
    }

    public Instant refundAuthorizedAt() {
        return refundAuthorizedAt;
    }

    public String refundAuthorizationNote() {
        return refundAuthorizationNote;
    }

    public String refundRejectedBy() {
        return refundRejectedBy;
    }

    public Instant refundRejectedAt() {
        return refundRejectedAt;
    }

    public String refundRejectionReason() {
        return refundRejectionReason;
    }

    public BigDecimal refundedAmount() {
        return refundedAmount;
    }

    public String refundReason() {
        return refundReason;
    }

    public String refundedBy() {
        return refundedBy;
    }

    public String refundMethod() {
        return refundMethod;
    }

    public Instant refundedAt() {
        return refundedAt;
    }

    public String finalizedBy() {
        return finalizedBy;
    }

    public Instant finalizedAt() {
        return finalizedAt;
    }

    public String holderDocument() {
        return holderDocument;
    }

    public List<Companion> companions() {
        return companions;
    }
}
