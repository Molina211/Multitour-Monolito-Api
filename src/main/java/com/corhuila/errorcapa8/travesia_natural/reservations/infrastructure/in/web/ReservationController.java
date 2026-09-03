package com.corhuila.errorcapa8.travesia_natural.reservations.infrastructure.in.web;

import com.corhuila.errorcapa8.travesia_natural.reservations.domain.exception.InvalidReservationException;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.exception.ReservationNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.Reservation;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.ReservedService;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.in.CreateReservationCommand;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.in.CreateReservationUseCase;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.in.ReservationQueryUseCase;
import com.corhuila.errorcapa8.travesia_natural.common.web.dto.ErrorResponse;
import com.corhuila.errorcapa8.travesia_natural.reservations.infrastructure.in.web.dto.CreateReservationRequest;
import com.corhuila.errorcapa8.travesia_natural.reservations.infrastructure.in.web.dto.ReservationResponse;
import com.corhuila.errorcapa8.travesia_natural.reservations.infrastructure.in.web.dto.ReservedServiceRequest;
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

@RestController
@RequestMapping("/api/tenants/{tenantId}/reservations")
public class ReservationController {

    private final CreateReservationUseCase createReservationUseCase;
    private final ReservationQueryUseCase reservationQueryUseCase;

    public ReservationController(CreateReservationUseCase createReservationUseCase,
                                  ReservationQueryUseCase reservationQueryUseCase) {
        this.createReservationUseCase = createReservationUseCase;
        this.reservationQueryUseCase = reservationQueryUseCase;
    }

    @PostMapping
    public ResponseEntity<ReservationResponse> create(@PathVariable String tenantId,
                                                        @RequestBody CreateReservationRequest request) {
        List<ReservedService> reservedServices = toDomainReservedServices(request.reservedServices());

        CreateReservationCommand command = new CreateReservationCommand(
                tenantId, request.customerId(), request.projectedValue(), reservedServices);

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

    private List<ReservedService> toDomainReservedServices(List<ReservedServiceRequest> requests) {
        if (requests == null) {
            return List.of();
        }
        return requests.stream()
                .map(r -> new ReservedService(r.serviceReference(), r.partySize(), r.scheduledDate()))
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
}
