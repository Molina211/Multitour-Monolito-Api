package com.corhuila.errorcapa8.travesia_natural.reservations.infrastructure.in.web;

import com.corhuila.errorcapa8.travesia_natural.common.security.JwtPrincipal;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.exception.InvalidReservationException;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.exception.ReservationNotCancellableException;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.exception.ReservationNotFinalizableException;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.exception.ReservationNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.exception.ReservationNotRefundableException;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.exception.TenantMismatchException;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.Companion;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.Reservation;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.ReservedService;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.in.CancelReservationCommand;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.in.CancelReservationUseCase;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.in.CreateReservationCommand;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.in.CreateReservationUseCase;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.in.FinalizeReservationCommand;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.in.FinalizeReservationUseCase;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.in.RefundReservationCommand;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.in.RefundReservationUseCase;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.in.ReservationQueryUseCase;
import com.corhuila.errorcapa8.travesia_natural.common.web.dto.ErrorResponse;
import com.corhuila.errorcapa8.travesia_natural.reservations.infrastructure.in.web.dto.CancelReservationRequest;
import com.corhuila.errorcapa8.travesia_natural.reservations.infrastructure.in.web.dto.CompanionRequest;
import com.corhuila.errorcapa8.travesia_natural.reservations.infrastructure.in.web.dto.CreateReservationRequest;
import com.corhuila.errorcapa8.travesia_natural.reservations.infrastructure.in.web.dto.FinalizeReservationRequest;
import com.corhuila.errorcapa8.travesia_natural.reservations.infrastructure.in.web.dto.RefundReservationRequest;
import com.corhuila.errorcapa8.travesia_natural.reservations.infrastructure.in.web.dto.ReservationResponse;
import com.corhuila.errorcapa8.travesia_natural.reservations.infrastructure.in.web.dto.ReservedServiceRequest;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantInactiveException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/*
 * ============================================================================================
 * LEE ESTO ANTES DE CONECTAR EL FRONTEND A `POST .../reservations` (spec 007)
 * ============================================================================================
 *
 * Desde esta spec, crear una reserva EXIGE un header `Authorization: Bearer <token>` con un
 * JWT emitido por `POST /api/tenants/{tenantId}/login` (spec 004). El `customerId` de la
 * reserva ya NO se envía en el body: lo toma el Backend del propio token (`sub`), así que
 * nadie puede crear una reserva a nombre de otro `customerId` con solo conocer su id.
 *
 * Esto significa que, tal como está hoy el repo Frontend, es imposible invocar este endpoint
 * de verdad: `login.component.ts` no guarda el JWT que devuelve el login (lo descarta) ni lo
 * reenvía en ninguna llamada posterior — el mismo hueco ya documentado en spec 004. Quien
 * integre Frontend + Backend (persona o IA) necesita:
 *   1. Guardar el `accessToken` que devuelve el login (spec 004) en algún almacenamiento del
 *      cliente (ej. memoria de sesión, no necesariamente localStorage).
 *   2. Reenviarlo como `Authorization: Bearer <token>` en `POST .../reservations`.
 * Ninguna de las dos cosas se implementa aquí — se documenta para que quede constancia de qué
 * falta, siguiendo la misma disciplina de las specs 003-006 (documentar el hueco, no
 * inventarlo ni implementarlo sin una HU de Frontend que lo pida).
 * ============================================================================================
 */
@RestController
@RequestMapping("/api/tenants/{tenantId}/reservations")
public class ReservationController {

    private final CreateReservationUseCase createReservationUseCase;
    private final ReservationQueryUseCase reservationQueryUseCase;
    private final CancelReservationUseCase cancelReservationUseCase;
    private final RefundReservationUseCase refundReservationUseCase;
    private final FinalizeReservationUseCase finalizeReservationUseCase;

    public ReservationController(CreateReservationUseCase createReservationUseCase,
                                  ReservationQueryUseCase reservationQueryUseCase,
                                  CancelReservationUseCase cancelReservationUseCase,
                                  RefundReservationUseCase refundReservationUseCase,
                                  FinalizeReservationUseCase finalizeReservationUseCase) {
        this.createReservationUseCase = createReservationUseCase;
        this.reservationQueryUseCase = reservationQueryUseCase;
        this.cancelReservationUseCase = cancelReservationUseCase;
        this.refundReservationUseCase = refundReservationUseCase;
        this.finalizeReservationUseCase = finalizeReservationUseCase;
    }

    @PostMapping
    public ResponseEntity<ReservationResponse> create(@PathVariable String tenantId,
                                                        @RequestBody CreateReservationRequest request,
                                                        Authentication authentication) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        if (!principal.tenantId().equals(tenantId)) {
            throw new TenantMismatchException();
        }

        List<ReservedService> reservedServices = toDomainReservedServices(request.reservedServices());
        List<Companion> companions = toDomainCompanions(request.companions());

        CreateReservationCommand command = new CreateReservationCommand(
                tenantId, principal.membershipId(), request.projectedValue(), reservedServices,
                request.holderDocument(), companions);

        Reservation reservation = createReservationUseCase.createReservation(command);

        return ResponseEntity.status(HttpStatus.CREATED).body(ReservationResponse.from(reservation));
    }

    @GetMapping
    public ResponseEntity<List<ReservationResponse>> listByTenant(@PathVariable String tenantId) {
        List<ReservationResponse> reservations = reservationQueryUseCase.listByTenant(tenantId).stream()
                .map(ReservationResponse::from)
                .toList();

        return ResponseEntity.ok(reservations);
    }

    @GetMapping("/{reservationId}")
    public ResponseEntity<ReservationResponse> getById(@PathVariable String tenantId,
                                                         @PathVariable UUID reservationId) {
        return ResponseEntity.ok(ReservationResponse.from(reservationQueryUseCase.getById(tenantId, reservationId)));
    }

    @GetMapping("/me")
    public ResponseEntity<List<ReservationResponse>> listMine(@PathVariable String tenantId,
                                                                 Authentication authentication) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        if (!principal.tenantId().equals(tenantId)) {
            throw new TenantMismatchException();
        }

        List<ReservationResponse> reservations = reservationQueryUseCase
                .listByTenantAndCustomer(tenantId, principal.membershipId()).stream()
                .map(ReservationResponse::from)
                .toList();

        return ResponseEntity.ok(reservations);
    }

    @GetMapping("/me/{reservationId}")
    public ResponseEntity<ReservationResponse> getMineById(@PathVariable String tenantId,
                                                              @PathVariable UUID reservationId,
                                                              Authentication authentication) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        if (!principal.tenantId().equals(tenantId)) {
            throw new TenantMismatchException();
        }

        Reservation reservation = reservationQueryUseCase
                .getByIdForCustomer(tenantId, principal.membershipId(), reservationId);

        return ResponseEntity.ok(ReservationResponse.from(reservation));
    }

    @PostMapping("/{reservationId}/cancel")
    public ResponseEntity<ReservationResponse> cancel(@PathVariable String tenantId,
                                                         @PathVariable UUID reservationId,
                                                         @RequestBody CancelReservationRequest request) {
        CancelReservationCommand command = new CancelReservationCommand(
                tenantId, reservationId, request.reason(), request.actorId());

        Reservation reservation = cancelReservationUseCase.cancelReservation(command);

        return ResponseEntity.ok(ReservationResponse.from(reservation));
    }

    @PostMapping("/{reservationId}/refund")
    public ResponseEntity<ReservationResponse> refund(@PathVariable String tenantId,
                                                         @PathVariable UUID reservationId,
                                                         @RequestBody RefundReservationRequest request) {
        RefundReservationCommand command = new RefundReservationCommand(
                tenantId, reservationId, request.amount(), request.reason(), request.actorId(), request.method());

        Reservation reservation = refundReservationUseCase.refundReservation(command);

        return ResponseEntity.ok(ReservationResponse.from(reservation));
    }

    @PostMapping("/{reservationId}/finalize")
    public ResponseEntity<ReservationResponse> finalize(@PathVariable String tenantId,
                                                           @PathVariable UUID reservationId,
                                                           @RequestBody FinalizeReservationRequest request) {
        FinalizeReservationCommand command = new FinalizeReservationCommand(
                tenantId, reservationId, request.actorId());

        Reservation reservation = finalizeReservationUseCase.finalizeReservation(command);

        return ResponseEntity.ok(ReservationResponse.from(reservation));
    }

    private List<ReservedService> toDomainReservedServices(List<ReservedServiceRequest> requests) {
        if (requests == null) {
            return List.of();
        }
        return requests.stream()
                .map(r -> new ReservedService(r.serviceReference(), r.partySize(), r.scheduledDate()))
                .toList();
    }

    private List<Companion> toDomainCompanions(List<CompanionRequest> requests) {
        if (requests == null) {
            return List.of();
        }
        return requests.stream()
                .map(r -> new Companion(r.name(), r.document(), r.birthDate()))
                .toList();
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

    @ExceptionHandler(TenantMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTenantMismatch(TenantMismatchException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse("tenant_mismatch", ex.getMessage()));
    }

    @ExceptionHandler(ReservationNotCancellableException.class)
    public ResponseEntity<ErrorResponse> handleReservationNotCancellable(ReservationNotCancellableException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("reservation_not_cancellable", ex.getMessage()));
    }

    @ExceptionHandler(ReservationNotRefundableException.class)
    public ResponseEntity<ErrorResponse> handleReservationNotRefundable(ReservationNotRefundableException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("reservation_not_refundable", ex.getMessage()));
    }

    @ExceptionHandler(ReservationNotFinalizableException.class)
    public ResponseEntity<ErrorResponse> handleReservationNotFinalizable(ReservationNotFinalizableException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("reservation_not_finalizable", ex.getMessage()));
    }
}
