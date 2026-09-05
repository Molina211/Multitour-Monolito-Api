package com.corhuila.errorcapa8.travesia_natural.tenants.infrastructure.in.web;

import com.corhuila.errorcapa8.travesia_natural.common.web.dto.ErrorResponse;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.CollaboratorNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.EmailAlreadyRegisteredException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.InvalidTenantException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantInactiveException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.Membership;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.in.CollaboratorQueryUseCase;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.in.RegisterCollaboratorCommand;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.in.RegisterCollaboratorUseCase;
import com.corhuila.errorcapa8.travesia_natural.tenants.infrastructure.in.web.dto.CollaboratorResponse;
import com.corhuila.errorcapa8.travesia_natural.tenants.infrastructure.in.web.dto.RegisterCollaboratorRequest;
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
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/tenants/{tenantId}/collaborators")
public class CollaboratorController {

    private final RegisterCollaboratorUseCase registerCollaboratorUseCase;
    private final CollaboratorQueryUseCase collaboratorQueryUseCase;

    public CollaboratorController(RegisterCollaboratorUseCase registerCollaboratorUseCase,
                                   CollaboratorQueryUseCase collaboratorQueryUseCase) {
        this.registerCollaboratorUseCase = registerCollaboratorUseCase;
        this.collaboratorQueryUseCase = collaboratorQueryUseCase;
    }

    @PostMapping
    public ResponseEntity<CollaboratorResponse> register(@PathVariable String tenantId,
                                                           @RequestBody RegisterCollaboratorRequest request) {
        if (!Objects.equals(request.password(), request.passwordConfirmation())) {
            throw new InvalidTenantException("password and passwordConfirmation must match");
        }

        RegisterCollaboratorCommand command = new RegisterCollaboratorCommand(
                tenantId, request.name(), request.email(), request.password(), request.actorId());

        Membership collaborator = registerCollaboratorUseCase.registerCollaborator(command);

        return ResponseEntity.status(HttpStatus.CREATED).body(CollaboratorResponse.from(collaborator));
    }

    @GetMapping
    public ResponseEntity<List<CollaboratorResponse>> list(@PathVariable String tenantId) {
        List<CollaboratorResponse> collaborators = collaboratorQueryUseCase.listByTenant(tenantId).stream()
                .map(CollaboratorResponse::from)
                .toList();

        return ResponseEntity.ok(collaborators);
    }

    @GetMapping("/{membershipId}")
    public ResponseEntity<CollaboratorResponse> getById(@PathVariable String tenantId,
                                                          @PathVariable UUID membershipId) {
        return ResponseEntity.ok(CollaboratorResponse.from(collaboratorQueryUseCase.getById(tenantId, membershipId)));
    }

    @ExceptionHandler({InvalidTenantException.class, IllegalArgumentException.class})
    public ResponseEntity<ErrorResponse> handleValidationError(RuntimeException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse("validation_error", ex.getMessage()));
    }

    @ExceptionHandler(TenantNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTenantNotFound(TenantNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("tenant_not_found", ex.getMessage()));
    }

    @ExceptionHandler(TenantInactiveException.class)
    public ResponseEntity<ErrorResponse> handleTenantInactive(TenantInactiveException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse("tenant_inactive", ex.getMessage()));
    }

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyRegistered(EmailAlreadyRegisteredException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse("email_already_registered", ex.getMessage()));
    }

    @ExceptionHandler(CollaboratorNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCollaboratorNotFound(CollaboratorNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("collaborator_not_found", ex.getMessage()));
    }
}
