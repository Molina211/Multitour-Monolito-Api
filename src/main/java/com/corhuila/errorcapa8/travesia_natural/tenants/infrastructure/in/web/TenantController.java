package com.corhuila.errorcapa8.travesia_natural.tenants.infrastructure.in.web;

import com.corhuila.errorcapa8.travesia_natural.common.web.dto.ErrorResponse;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.InvalidTenantException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantAlreadyExistsException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantInactiveException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantPermissionNotAllowedException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.Tenant;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.in.CreateTenantCommand;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.in.CreateTenantUseCase;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.in.DeactivateTenantCommand;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.in.DeactivateTenantUseCase;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.in.ReactivateTenantCommand;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.in.ReactivateTenantUseCase;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.in.TenantQueryUseCase;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.in.UpdateCollaboratorSupportValidationPermissionCommand;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.in.UpdateCollaboratorSupportValidationPermissionUseCase;
import com.corhuila.errorcapa8.travesia_natural.tenants.infrastructure.in.web.dto.CreateTenantRequest;
import com.corhuila.errorcapa8.travesia_natural.tenants.infrastructure.in.web.dto.TenantLifecycleRequest;
import com.corhuila.errorcapa8.travesia_natural.tenants.infrastructure.in.web.dto.TenantResponse;
import com.corhuila.errorcapa8.travesia_natural.tenants.infrastructure.in.web.dto.UpdateCollaboratorSupportValidationPermissionRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/tenants")
public class TenantController {

    private final CreateTenantUseCase createTenantUseCase;
    private final DeactivateTenantUseCase deactivateTenantUseCase;
    private final ReactivateTenantUseCase reactivateTenantUseCase;
    private final TenantQueryUseCase tenantQueryUseCase;
    private final UpdateCollaboratorSupportValidationPermissionUseCase
            updateCollaboratorSupportValidationPermissionUseCase;

    public TenantController(CreateTenantUseCase createTenantUseCase,
                             DeactivateTenantUseCase deactivateTenantUseCase,
                             ReactivateTenantUseCase reactivateTenantUseCase,
                             TenantQueryUseCase tenantQueryUseCase,
                             UpdateCollaboratorSupportValidationPermissionUseCase
                                     updateCollaboratorSupportValidationPermissionUseCase) {
        this.createTenantUseCase = createTenantUseCase;
        this.deactivateTenantUseCase = deactivateTenantUseCase;
        this.reactivateTenantUseCase = reactivateTenantUseCase;
        this.tenantQueryUseCase = tenantQueryUseCase;
        this.updateCollaboratorSupportValidationPermissionUseCase =
                updateCollaboratorSupportValidationPermissionUseCase;
    }

    @PostMapping
    public ResponseEntity<TenantResponse> create(@RequestBody CreateTenantRequest request) {
        if (request.administrator() == null) {
            throw new InvalidTenantException("administrator data is required");
        }
        if (!Objects.equals(request.administrator().password(), request.administrator().passwordConfirmation())) {
            throw new InvalidTenantException("password and passwordConfirmation must match");
        }

        CreateTenantCommand command = new CreateTenantCommand(
                request.tenantId(),
                request.commercialName(),
                request.administrator().email(),
                request.administrator().password(),
                request.actorId());

        Tenant tenant = createTenantUseCase.createTenant(command);

        return ResponseEntity.status(HttpStatus.CREATED).body(TenantResponse.from(tenant));
    }

    @PostMapping("/{tenantId}/deactivate")
    public ResponseEntity<TenantResponse> deactivate(@PathVariable String tenantId,
                                                       @RequestBody TenantLifecycleRequest request) {
        Tenant tenant = deactivateTenantUseCase.deactivateTenant(
                new DeactivateTenantCommand(tenantId, request.reason(), request.actorId()));

        return ResponseEntity.ok(TenantResponse.from(tenant));
    }

    @PostMapping("/{tenantId}/reactivate")
    public ResponseEntity<TenantResponse> reactivate(@PathVariable String tenantId,
                                                       @RequestBody TenantLifecycleRequest request) {
        Tenant tenant = reactivateTenantUseCase.reactivateTenant(
                new ReactivateTenantCommand(tenantId, request.reason(), request.actorId()));

        return ResponseEntity.ok(TenantResponse.from(tenant));
    }

    @GetMapping
    public ResponseEntity<List<TenantResponse>> listAll() {
        List<TenantResponse> tenants = tenantQueryUseCase.listAll().stream()
                .map(TenantResponse::from)
                .toList();

        return ResponseEntity.ok(tenants);
    }

    @GetMapping("/{tenantId}")
    public ResponseEntity<TenantResponse> getById(@PathVariable String tenantId) {
        return ResponseEntity.ok(TenantResponse.from(tenantQueryUseCase.getById(tenantId)));
    }

    @PatchMapping("/{tenantId}/collaborator-support-permission")
    public ResponseEntity<TenantResponse> updateCollaboratorSupportValidationPermission(
            @PathVariable String tenantId,
            @RequestBody UpdateCollaboratorSupportValidationPermissionRequest request) {
        Tenant tenant = updateCollaboratorSupportValidationPermissionUseCase
                .updateCollaboratorSupportValidationPermission(
                        new UpdateCollaboratorSupportValidationPermissionCommand(
                                tenantId, request.actorId(), request.allow()));

        return ResponseEntity.ok(TenantResponse.from(tenant));
    }

    @ExceptionHandler({InvalidTenantException.class, IllegalArgumentException.class})
    public ResponseEntity<ErrorResponse> handleValidationError(RuntimeException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse("validation_error", ex.getMessage()));
    }

    @ExceptionHandler(TenantAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleAlreadyExists(TenantAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse("tenant_already_exists", ex.getMessage()));
    }

    @ExceptionHandler(TenantNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(TenantNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("tenant_not_found", ex.getMessage()));
    }

    @ExceptionHandler(TenantPermissionNotAllowedException.class)
    public ResponseEntity<ErrorResponse> handlePermissionNotAllowed(TenantPermissionNotAllowedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("tenant_permission_not_allowed", ex.getMessage()));
    }

    @ExceptionHandler(TenantInactiveException.class)
    public ResponseEntity<ErrorResponse> handleTenantInactive(TenantInactiveException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse("tenant_inactive", ex.getMessage()));
    }
}
