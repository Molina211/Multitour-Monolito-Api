package com.corhuila.errorcapa8.travesia_natural.cash.infrastructure.in.web;

import com.corhuila.errorcapa8.travesia_natural.cash.domain.exception.CashRegisterAlreadyOpenException;
import com.corhuila.errorcapa8.travesia_natural.cash.domain.exception.CashRegisterClosedException;
import com.corhuila.errorcapa8.travesia_natural.cash.domain.exception.CashRegisterNotClosedException;
import com.corhuila.errorcapa8.travesia_natural.cash.domain.exception.CashRegisterNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.cash.domain.model.CashMovementType;
import com.corhuila.errorcapa8.travesia_natural.cash.domain.model.CashRegister;
import com.corhuila.errorcapa8.travesia_natural.cash.domain.port.in.AddCashCorrectionCommand;
import com.corhuila.errorcapa8.travesia_natural.cash.domain.port.in.AddCashCorrectionUseCase;
import com.corhuila.errorcapa8.travesia_natural.cash.domain.port.in.CashRegisterQueryUseCase;
import com.corhuila.errorcapa8.travesia_natural.cash.domain.port.in.CloseCashRegisterCommand;
import com.corhuila.errorcapa8.travesia_natural.cash.domain.port.in.CloseCashRegisterUseCase;
import com.corhuila.errorcapa8.travesia_natural.cash.domain.port.in.MonthlyCashConsolidationQueryUseCase;
import com.corhuila.errorcapa8.travesia_natural.cash.domain.port.in.OpenCashRegisterCommand;
import com.corhuila.errorcapa8.travesia_natural.cash.domain.port.in.OpenCashRegisterUseCase;
import com.corhuila.errorcapa8.travesia_natural.cash.domain.port.in.RegisterCashMovementCommand;
import com.corhuila.errorcapa8.travesia_natural.cash.domain.port.in.RegisterCashMovementUseCase;
import com.corhuila.errorcapa8.travesia_natural.cash.infrastructure.in.web.dto.AddCashCorrectionRequest;
import com.corhuila.errorcapa8.travesia_natural.cash.infrastructure.in.web.dto.CashRegisterResponse;
import com.corhuila.errorcapa8.travesia_natural.cash.infrastructure.in.web.dto.CloseCashRegisterRequest;
import com.corhuila.errorcapa8.travesia_natural.cash.infrastructure.in.web.dto.MonthlyConsolidationResponse;
import com.corhuila.errorcapa8.travesia_natural.cash.infrastructure.in.web.dto.OpenCashRegisterRequest;
import com.corhuila.errorcapa8.travesia_natural.cash.infrastructure.in.web.dto.RegisterCashMovementRequest;
import com.corhuila.errorcapa8.travesia_natural.common.web.dto.ErrorResponse;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/**
 * Sin JWT (spec 010/013, mismo criterio que el resto del proyecto): son operaciones de
 * operador/staff, que no tiene ningún mecanismo de login todavía.
 */
@RestController
@RequestMapping("/api/tenants/{tenantId}/cash")
public class CashController {

    private final OpenCashRegisterUseCase openCashRegisterUseCase;
    private final RegisterCashMovementUseCase registerCashMovementUseCase;
    private final CloseCashRegisterUseCase closeCashRegisterUseCase;
    private final AddCashCorrectionUseCase addCashCorrectionUseCase;
    private final CashRegisterQueryUseCase cashRegisterQueryUseCase;
    private final MonthlyCashConsolidationQueryUseCase monthlyCashConsolidationQueryUseCase;

    public CashController(OpenCashRegisterUseCase openCashRegisterUseCase,
                           RegisterCashMovementUseCase registerCashMovementUseCase,
                           CloseCashRegisterUseCase closeCashRegisterUseCase,
                           AddCashCorrectionUseCase addCashCorrectionUseCase,
                           CashRegisterQueryUseCase cashRegisterQueryUseCase,
                           MonthlyCashConsolidationQueryUseCase monthlyCashConsolidationQueryUseCase) {
        this.openCashRegisterUseCase = openCashRegisterUseCase;
        this.registerCashMovementUseCase = registerCashMovementUseCase;
        this.closeCashRegisterUseCase = closeCashRegisterUseCase;
        this.addCashCorrectionUseCase = addCashCorrectionUseCase;
        this.cashRegisterQueryUseCase = cashRegisterQueryUseCase;
        this.monthlyCashConsolidationQueryUseCase = monthlyCashConsolidationQueryUseCase;
    }

    @PostMapping
    public ResponseEntity<CashRegisterResponse> open(@PathVariable String tenantId,
                                                       @RequestBody OpenCashRegisterRequest request) {
        OpenCashRegisterCommand command = new OpenCashRegisterCommand(tenantId, request.businessDate(),
                request.baseAmount());

        CashRegister cashRegister = openCashRegisterUseCase.openCashRegister(command);

        return ResponseEntity.status(HttpStatus.CREATED).body(CashRegisterResponse.from(cashRegister));
    }

    @PostMapping("/{cashRegisterId}/movements")
    public ResponseEntity<CashRegisterResponse> registerMovement(@PathVariable String tenantId,
                                                                   @PathVariable UUID cashRegisterId,
                                                                   @RequestBody RegisterCashMovementRequest request) {
        RegisterCashMovementCommand command = new RegisterCashMovementCommand(tenantId, cashRegisterId,
                CashMovementType.fromLabel(request.type()), request.amount(), request.concept(), request.actorId());

        CashRegister cashRegister = registerCashMovementUseCase.registerMovement(command);

        return ResponseEntity.status(HttpStatus.CREATED).body(CashRegisterResponse.from(cashRegister));
    }

    @PostMapping("/{cashRegisterId}/close")
    public ResponseEntity<CashRegisterResponse> close(@PathVariable String tenantId,
                                                        @PathVariable UUID cashRegisterId,
                                                        @RequestBody CloseCashRegisterRequest request) {
        CloseCashRegisterCommand command = new CloseCashRegisterCommand(tenantId, cashRegisterId,
                request.actorId());

        CashRegister cashRegister = closeCashRegisterUseCase.closeCashRegister(command);

        return ResponseEntity.ok(CashRegisterResponse.from(cashRegister));
    }

    @PostMapping("/{cashRegisterId}/corrections")
    public ResponseEntity<CashRegisterResponse> addCorrection(@PathVariable String tenantId,
                                                                @PathVariable UUID cashRegisterId,
                                                                @RequestBody AddCashCorrectionRequest request) {
        AddCashCorrectionCommand command = new AddCashCorrectionCommand(tenantId, cashRegisterId,
                request.justification(), request.actorId());

        CashRegister cashRegister = addCashCorrectionUseCase.addCorrection(command);

        return ResponseEntity.status(HttpStatus.CREATED).body(CashRegisterResponse.from(cashRegister));
    }

    @GetMapping
    public ResponseEntity<CashRegisterResponse> getByBusinessDate(@PathVariable String tenantId,
                                                                    @RequestParam LocalDate businessDate) {
        CashRegister cashRegister = cashRegisterQueryUseCase.getByBusinessDate(tenantId, businessDate);

        return ResponseEntity.ok(CashRegisterResponse.from(cashRegister));
    }

    @GetMapping("/history")
    public ResponseEntity<List<CashRegisterResponse>> listHistory(@PathVariable String tenantId) {
        List<CashRegisterResponse> history = cashRegisterQueryUseCase.listHistory(tenantId).stream()
                .map(CashRegisterResponse::from)
                .toList();

        return ResponseEntity.ok(history);
    }

    @GetMapping("/consolidation")
    public ResponseEntity<List<MonthlyConsolidationResponse>> getMonthlyConsolidation(
            @PathVariable String tenantId, @RequestParam YearMonth period) {
        List<MonthlyConsolidationResponse> consolidation = monthlyCashConsolidationQueryUseCase
                .getMonthlyConsolidation(tenantId, period).stream()
                .map(MonthlyConsolidationResponse::from)
                .toList();

        return ResponseEntity.ok(consolidation);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleValidationError(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse("validation_error", ex.getMessage()));
    }

    @ExceptionHandler({TenantNotFoundException.class, CashRegisterNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("not_found", ex.getMessage()));
    }

    @ExceptionHandler(TenantInactiveException.class)
    public ResponseEntity<ErrorResponse> handleTenantInactive(TenantInactiveException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse("tenant_inactive", ex.getMessage()));
    }

    @ExceptionHandler({CashRegisterAlreadyOpenException.class, CashRegisterClosedException.class,
            CashRegisterNotClosedException.class})
    public ResponseEntity<ErrorResponse> handleConflict(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse("conflict", ex.getMessage()));
    }
}
