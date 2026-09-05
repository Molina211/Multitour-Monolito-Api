package com.corhuila.errorcapa8.travesia_natural.reservations.infrastructure.in.web;

import com.corhuila.errorcapa8.travesia_natural.common.web.dto.ErrorResponse;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.exception.InvalidReservationException;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.exception.PaymentAlreadyResolvedException;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.exception.ReservationNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.Reservation;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.in.DecidePaymentSupportCommand;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.in.DecidePaymentSupportUseCase;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.in.PaymentFollowupQueryUseCase;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.in.RegisterPaymentCommand;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.in.RegisterPaymentFollowupCommand;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.in.RegisterPaymentFollowupUseCase;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.in.RegisterPaymentUseCase;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.in.ReservationQueryUseCase;
import com.corhuila.errorcapa8.travesia_natural.reservations.infrastructure.in.web.dto.DecidePaymentSupportRequest;
import com.corhuila.errorcapa8.travesia_natural.reservations.infrastructure.in.web.dto.PaymentFollowupRequest;
import com.corhuila.errorcapa8.travesia_natural.reservations.infrastructure.in.web.dto.PaymentFollowupResponse;
import com.corhuila.errorcapa8.travesia_natural.reservations.infrastructure.in.web.dto.RegisterPaymentRequest;
import com.corhuila.errorcapa8.travesia_natural.reservations.infrastructure.in.web.dto.ReservationResponse;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantInactiveException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Sin JWT (spec 009, decisión técnica 3): son operaciones de operador/staff, que no
 * tiene ningún mecanismo de login todavía (mismo hueco documentado desde spec 004).
 */
@RestController
@RequestMapping("/api/tenants/{tenantId}/reservations")
public class PaymentController {

    private final RegisterPaymentUseCase registerPaymentUseCase;
    private final DecidePaymentSupportUseCase decidePaymentSupportUseCase;
    private final ReservationQueryUseCase reservationQueryUseCase;
    private final RegisterPaymentFollowupUseCase registerPaymentFollowupUseCase;
    private final PaymentFollowupQueryUseCase paymentFollowupQueryUseCase;

    public PaymentController(RegisterPaymentUseCase registerPaymentUseCase,
                              DecidePaymentSupportUseCase decidePaymentSupportUseCase,
                              ReservationQueryUseCase reservationQueryUseCase,
                              RegisterPaymentFollowupUseCase registerPaymentFollowupUseCase,
                              PaymentFollowupQueryUseCase paymentFollowupQueryUseCase) {
        this.registerPaymentUseCase = registerPaymentUseCase;
        this.decidePaymentSupportUseCase = decidePaymentSupportUseCase;
        this.reservationQueryUseCase = reservationQueryUseCase;
        this.registerPaymentFollowupUseCase = registerPaymentFollowupUseCase;
        this.paymentFollowupQueryUseCase = paymentFollowupQueryUseCase;
    }

    @PostMapping("/{reservationId}/payments")
    public ResponseEntity<ReservationResponse> registerPayment(@PathVariable String tenantId,
                                                                 @PathVariable UUID reservationId,
                                                                 @RequestBody RegisterPaymentRequest request) {
        RegisterPaymentCommand command = new RegisterPaymentCommand(
                tenantId, reservationId, request.method(), request.amount(), request.supportReference());

        Reservation reservation = registerPaymentUseCase.registerPayment(command);

        return ResponseEntity.ok(ReservationResponse.from(reservation));
    }

    @PostMapping("/{reservationId}/payments/decide-support")
    public ResponseEntity<ReservationResponse> decideSupport(@PathVariable String tenantId,
                                                               @PathVariable UUID reservationId,
                                                               @RequestBody DecidePaymentSupportRequest request) {
        DecidePaymentSupportCommand command = new DecidePaymentSupportCommand(
                tenantId, reservationId, request.decision(), request.reason(), request.actorId());

        Reservation reservation = decidePaymentSupportUseCase.decidePaymentSupport(command);

        return ResponseEntity.ok(ReservationResponse.from(reservation));
    }

    @GetMapping("/pending-support")
    public ResponseEntity<List<ReservationResponse>> listPendingSupport(@PathVariable String tenantId) {
        List<ReservationResponse> reservations = reservationQueryUseCase.listPendingSupportByTenant(tenantId)
                .stream()
                .map(ReservationResponse::from)
                .toList();

        return ResponseEntity.ok(reservations);
    }

    @PostMapping("/{reservationId}/payments/followups")
    public ResponseEntity<PaymentFollowupResponse> registerFollowup(@PathVariable String tenantId,
                                                                      @PathVariable UUID reservationId,
                                                                      @RequestBody PaymentFollowupRequest request) {
        RegisterPaymentFollowupCommand command = new RegisterPaymentFollowupCommand(
                tenantId, reservationId, request.note(), request.actorId());

        var record = registerPaymentFollowupUseCase.registerFollowup(command);

        return ResponseEntity.status(HttpStatus.CREATED).body(PaymentFollowupResponse.from(record));
    }

    @GetMapping("/{reservationId}/payments/followups")
    public ResponseEntity<List<PaymentFollowupResponse>> listFollowups(@PathVariable String tenantId,
                                                                         @PathVariable UUID reservationId) {
        List<PaymentFollowupResponse> followups = paymentFollowupQueryUseCase.listFollowups(tenantId, reservationId)
                .stream()
                .map(PaymentFollowupResponse::from)
                .toList();

        return ResponseEntity.ok(followups);
    }

    @ExceptionHandler({InvalidReservationException.class, IllegalArgumentException.class})
    public ResponseEntity<ErrorResponse> handleValidationError(RuntimeException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse("validation_error", ex.getMessage()));
    }

    @ExceptionHandler({TenantNotFoundException.class, ReservationNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("not_found", ex.getMessage()));
    }

    @ExceptionHandler(TenantInactiveException.class)
    public ResponseEntity<ErrorResponse> handleTenantInactive(TenantInactiveException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse("tenant_inactive", ex.getMessage()));
    }

    @ExceptionHandler(PaymentAlreadyResolvedException.class)
    public ResponseEntity<ErrorResponse> handlePaymentAlreadyResolved(PaymentAlreadyResolvedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse("payment_already_resolved", ex.getMessage()));
    }
}
