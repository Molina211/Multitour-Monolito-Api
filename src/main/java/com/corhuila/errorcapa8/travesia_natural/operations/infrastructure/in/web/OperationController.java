package com.corhuila.errorcapa8.travesia_natural.operations.infrastructure.in.web;

import com.corhuila.errorcapa8.travesia_natural.common.web.dto.ErrorResponse;
import com.corhuila.errorcapa8.travesia_natural.operations.domain.exception.ExecutionNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.operations.domain.exception.ExecutionNotStartedException;
import com.corhuila.errorcapa8.travesia_natural.operations.domain.model.Execution;
import com.corhuila.errorcapa8.travesia_natural.operations.domain.model.OperationCost;
import com.corhuila.errorcapa8.travesia_natural.operations.domain.port.in.ExecutionQueryUseCase;
import com.corhuila.errorcapa8.travesia_natural.operations.domain.port.in.OperationCostQueryUseCase;
import com.corhuila.errorcapa8.travesia_natural.operations.domain.port.in.RegisterExecutionCommand;
import com.corhuila.errorcapa8.travesia_natural.operations.domain.port.in.RegisterExecutionUseCase;
import com.corhuila.errorcapa8.travesia_natural.operations.domain.port.in.RegisterOperationCostCommand;
import com.corhuila.errorcapa8.travesia_natural.operations.domain.port.in.RegisterOperationCostUseCase;
import com.corhuila.errorcapa8.travesia_natural.operations.infrastructure.in.web.dto.ExecutionResponse;
import com.corhuila.errorcapa8.travesia_natural.operations.infrastructure.in.web.dto.OperationCostResponse;
import com.corhuila.errorcapa8.travesia_natural.operations.infrastructure.in.web.dto.RegisterExecutionRequest;
import com.corhuila.errorcapa8.travesia_natural.operations.infrastructure.in.web.dto.RegisterOperationCostRequest;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.exception.ReservationNotExecutableException;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.exception.ReservationNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.in.ReservationQueryUseCase;
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
 * Sin JWT (spec 010, mismo criterio que spec 009): son operaciones de operador/staff,
 * que no tiene ningún mecanismo de login todavía.
 */
@RestController
@RequestMapping("/api/tenants/{tenantId}/reservations")
public class OperationController {

    private final RegisterExecutionUseCase registerExecutionUseCase;
    private final ExecutionQueryUseCase executionQueryUseCase;
    private final ReservationQueryUseCase reservationQueryUseCase;
    private final RegisterOperationCostUseCase registerOperationCostUseCase;
    private final OperationCostQueryUseCase operationCostQueryUseCase;

    public OperationController(RegisterExecutionUseCase registerExecutionUseCase,
                                ExecutionQueryUseCase executionQueryUseCase,
                                ReservationQueryUseCase reservationQueryUseCase,
                                RegisterOperationCostUseCase registerOperationCostUseCase,
                                OperationCostQueryUseCase operationCostQueryUseCase) {
        this.registerExecutionUseCase = registerExecutionUseCase;
        this.executionQueryUseCase = executionQueryUseCase;
        this.reservationQueryUseCase = reservationQueryUseCase;
        this.registerOperationCostUseCase = registerOperationCostUseCase;
        this.operationCostQueryUseCase = operationCostQueryUseCase;
    }

    @PostMapping("/{reservationId}/execution")
    public ResponseEntity<ExecutionResponse> registerExecution(@PathVariable String tenantId,
                                                                 @PathVariable UUID reservationId,
                                                                 @RequestBody RegisterExecutionRequest request) {
        RegisterExecutionCommand command = new RegisterExecutionCommand(
                tenantId, reservationId, request.served(), request.executed(), request.causal(), request.actorId());

        Execution execution = registerExecutionUseCase.registerExecution(command);

        return ResponseEntity.ok(ExecutionResponse.from(execution));
    }

    @GetMapping("/{reservationId}/execution")
    public ResponseEntity<ExecutionResponse> getExecution(@PathVariable String tenantId,
                                                            @PathVariable UUID reservationId) {
        Execution execution = executionQueryUseCase.getByReservation(tenantId, reservationId);

        return ResponseEntity.ok(ExecutionResponse.from(execution));
    }

    @GetMapping("/pending-execution")
    public ResponseEntity<List<ReservationResponse>> listPendingExecution(@PathVariable String tenantId) {
        List<ReservationResponse> reservations = reservationQueryUseCase.listPendingExecutionByTenant(tenantId)
                .stream()
                .map(ReservationResponse::from)
                .toList();

        return ResponseEntity.ok(reservations);
    }

    @PostMapping("/{reservationId}/costs")
    public ResponseEntity<OperationCostResponse> registerCost(@PathVariable String tenantId,
                                                                @PathVariable UUID reservationId,
                                                                @RequestBody RegisterOperationCostRequest request) {
        RegisterOperationCostCommand command = new RegisterOperationCostCommand(
                tenantId, reservationId, request.concept(), request.amount(), request.actorId());

        OperationCost operationCost = registerOperationCostUseCase.registerCost(command);

        return ResponseEntity.status(HttpStatus.CREATED).body(OperationCostResponse.from(operationCost));
    }

    @GetMapping("/{reservationId}/costs")
    public ResponseEntity<List<OperationCostResponse>> listCosts(@PathVariable String tenantId,
                                                                   @PathVariable UUID reservationId) {
        List<OperationCostResponse> costs = operationCostQueryUseCase.listByReservation(tenantId, reservationId)
                .stream()
                .map(OperationCostResponse::from)
                .toList();

        return ResponseEntity.ok(costs);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleValidationError(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse("validation_error", ex.getMessage()));
    }

    @ExceptionHandler({TenantNotFoundException.class, ReservationNotFoundException.class,
            ExecutionNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("not_found", ex.getMessage()));
    }

    @ExceptionHandler(TenantInactiveException.class)
    public ResponseEntity<ErrorResponse> handleTenantInactive(TenantInactiveException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse("tenant_inactive", ex.getMessage()));
    }

    @ExceptionHandler(ReservationNotExecutableException.class)
    public ResponseEntity<ErrorResponse> handleReservationNotExecutable(ReservationNotExecutableException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("reservation_not_executable", ex.getMessage()));
    }

    @ExceptionHandler(ExecutionNotStartedException.class)
    public ResponseEntity<ErrorResponse> handleExecutionNotStarted(ExecutionNotStartedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("execution_not_started", ex.getMessage()));
    }
}
