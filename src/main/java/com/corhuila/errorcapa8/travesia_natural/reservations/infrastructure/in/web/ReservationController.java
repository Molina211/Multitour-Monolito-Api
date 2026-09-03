package com.corhuila.errorcapa8.travesia_natural.reservations.infrastructure.in.web;

import com.corhuila.errorcapa8.travesia_natural.reservations.domain.exception.InvalidReservationException;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.Reservation;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.ReservedService;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.in.CreateReservationCommand;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.in.CreateReservationUseCase;
import com.corhuila.errorcapa8.travesia_natural.common.web.dto.ErrorResponse;
import com.corhuila.errorcapa8.travesia_natural.reservations.infrastructure.in.web.dto.CreateReservationRequest;
import com.corhuila.errorcapa8.travesia_natural.reservations.infrastructure.in.web.dto.ReservationResponse;
import com.corhuila.errorcapa8.travesia_natural.reservations.infrastructure.in.web.dto.ReservedServiceRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * tenantId comes from the X-Tenant-Id header as a temporary trust mechanism
 * (spec 001, decision 1) until a JWT-based spec resolves it from an authenticated token.
 */
@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private static final String TENANT_HEADER = "X-Tenant-Id";

    private final CreateReservationUseCase createReservationUseCase;

    public ReservationController(CreateReservationUseCase createReservationUseCase) {
        this.createReservationUseCase = createReservationUseCase;
    }

    @PostMapping
    public ResponseEntity<ReservationResponse> create(
            @RequestHeader(value = TENANT_HEADER, required = false) String tenantIdHeader,
            @RequestBody CreateReservationRequest request) {

        UUID tenantId = parseTenantId(tenantIdHeader);
        List<ReservedService> reservedServices = toDomainReservedServices(request.reservedServices());

        CreateReservationCommand command = new CreateReservationCommand(
                tenantId, request.customerId(), request.projectedValue(), reservedServices);

        Reservation reservation = createReservationUseCase.createReservation(command);

        return ResponseEntity.status(HttpStatus.CREATED).body(ReservationResponse.from(reservation));
    }

    private UUID parseTenantId(String tenantIdHeader) {
        if (tenantIdHeader == null || tenantIdHeader.isBlank()) {
            throw new InvalidReservationException(TENANT_HEADER + " header is required");
        }
        try {
            return UUID.fromString(tenantIdHeader);
        } catch (IllegalArgumentException ex) {
            throw new InvalidReservationException(TENANT_HEADER + " must be a valid UUID");
        }
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
}
