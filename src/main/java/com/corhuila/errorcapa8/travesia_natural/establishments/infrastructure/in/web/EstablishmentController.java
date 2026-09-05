package com.corhuila.errorcapa8.travesia_natural.establishments.infrastructure.in.web;

import com.corhuila.errorcapa8.travesia_natural.common.web.dto.ErrorResponse;
import com.corhuila.errorcapa8.travesia_natural.establishments.domain.exception.EstablishmentNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.establishments.domain.exception.InvalidEstablishmentException;
import com.corhuila.errorcapa8.travesia_natural.establishments.domain.model.AssociatedEstablishment;
import com.corhuila.errorcapa8.travesia_natural.establishments.domain.port.in.CreateEstablishmentCommand;
import com.corhuila.errorcapa8.travesia_natural.establishments.domain.port.in.CreateEstablishmentUseCase;
import com.corhuila.errorcapa8.travesia_natural.establishments.domain.port.in.DeactivateEstablishmentUseCase;
import com.corhuila.errorcapa8.travesia_natural.establishments.domain.port.in.EstablishmentQueryUseCase;
import com.corhuila.errorcapa8.travesia_natural.establishments.domain.port.in.ReactivateEstablishmentUseCase;
import com.corhuila.errorcapa8.travesia_natural.establishments.infrastructure.in.web.dto.EstablishmentRequest;
import com.corhuila.errorcapa8.travesia_natural.establishments.infrastructure.in.web.dto.EstablishmentResponse;
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
@RequestMapping("/api/tenants/{tenantId}/establishments")
public class EstablishmentController {

    private final CreateEstablishmentUseCase createEstablishmentUseCase;
    private final EstablishmentQueryUseCase establishmentQueryUseCase;
    private final DeactivateEstablishmentUseCase deactivateEstablishmentUseCase;
    private final ReactivateEstablishmentUseCase reactivateEstablishmentUseCase;

    public EstablishmentController(CreateEstablishmentUseCase createEstablishmentUseCase,
                                    EstablishmentQueryUseCase establishmentQueryUseCase,
                                    DeactivateEstablishmentUseCase deactivateEstablishmentUseCase,
                                    ReactivateEstablishmentUseCase reactivateEstablishmentUseCase) {
        this.createEstablishmentUseCase = createEstablishmentUseCase;
        this.establishmentQueryUseCase = establishmentQueryUseCase;
        this.deactivateEstablishmentUseCase = deactivateEstablishmentUseCase;
        this.reactivateEstablishmentUseCase = reactivateEstablishmentUseCase;
    }

    @PostMapping
    public ResponseEntity<EstablishmentResponse> create(@PathVariable String tenantId,
                                                          @RequestBody EstablishmentRequest request) {
        CreateEstablishmentCommand command = new CreateEstablishmentCommand(
                tenantId, request.kind(), request.name(), request.description(), request.image());

        AssociatedEstablishment establishment = createEstablishmentUseCase.createEstablishment(command);

        return ResponseEntity.status(HttpStatus.CREATED).body(EstablishmentResponse.from(establishment));
    }

    @GetMapping
    public ResponseEntity<List<EstablishmentResponse>> listByTenant(@PathVariable String tenantId) {
        List<EstablishmentResponse> establishments = establishmentQueryUseCase.listByTenant(tenantId).stream()
                .map(EstablishmentResponse::from)
                .toList();

        return ResponseEntity.ok(establishments);
    }

    @PostMapping("/{establishmentId}/deactivate")
    public ResponseEntity<EstablishmentResponse> deactivate(@PathVariable String tenantId,
                                                              @PathVariable UUID establishmentId) {
        AssociatedEstablishment establishment =
                deactivateEstablishmentUseCase.deactivateEstablishment(tenantId, establishmentId);

        return ResponseEntity.ok(EstablishmentResponse.from(establishment));
    }

    @PostMapping("/{establishmentId}/reactivate")
    public ResponseEntity<EstablishmentResponse> reactivate(@PathVariable String tenantId,
                                                              @PathVariable UUID establishmentId) {
        AssociatedEstablishment establishment =
                reactivateEstablishmentUseCase.reactivateEstablishment(tenantId, establishmentId);

        return ResponseEntity.ok(EstablishmentResponse.from(establishment));
    }

    @ExceptionHandler({InvalidEstablishmentException.class, IllegalArgumentException.class})
    public ResponseEntity<ErrorResponse> handleValidationError(RuntimeException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse("validation_error", ex.getMessage()));
    }

    @ExceptionHandler({TenantNotFoundException.class, EstablishmentNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("not_found", ex.getMessage()));
    }

    @ExceptionHandler(TenantInactiveException.class)
    public ResponseEntity<ErrorResponse> handleTenantInactive(TenantInactiveException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse("tenant_inactive", ex.getMessage()));
    }
}
