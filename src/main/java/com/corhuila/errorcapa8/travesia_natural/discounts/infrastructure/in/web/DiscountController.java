package com.corhuila.errorcapa8.travesia_natural.discounts.infrastructure.in.web;

import com.corhuila.errorcapa8.travesia_natural.catalog.domain.exception.CatalogItemNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.common.web.dto.ErrorResponse;
import com.corhuila.errorcapa8.travesia_natural.discounts.domain.exception.DiscountNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.discounts.domain.exception.InvalidDiscountException;
import com.corhuila.errorcapa8.travesia_natural.discounts.domain.model.Discount;
import com.corhuila.errorcapa8.travesia_natural.discounts.domain.port.in.CreateDiscountCommand;
import com.corhuila.errorcapa8.travesia_natural.discounts.domain.port.in.CreateDiscountUseCase;
import com.corhuila.errorcapa8.travesia_natural.discounts.domain.port.in.DiscountQueryUseCase;
import com.corhuila.errorcapa8.travesia_natural.discounts.infrastructure.in.web.dto.DiscountRequest;
import com.corhuila.errorcapa8.travesia_natural.discounts.infrastructure.in.web.dto.DiscountResponse;
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
@RequestMapping("/api/tenants/{tenantId}/discounts")
public class DiscountController {

    private final CreateDiscountUseCase createDiscountUseCase;
    private final DiscountQueryUseCase discountQueryUseCase;

    public DiscountController(CreateDiscountUseCase createDiscountUseCase,
                               DiscountQueryUseCase discountQueryUseCase) {
        this.createDiscountUseCase = createDiscountUseCase;
        this.discountQueryUseCase = discountQueryUseCase;
    }

    @PostMapping
    public ResponseEntity<DiscountResponse> create(@PathVariable String tenantId,
                                                     @RequestBody DiscountRequest request) {
        CreateDiscountCommand command = new CreateDiscountCommand(
                tenantId, request.catalogItemId(), request.percentage(), request.validFrom(), request.validTo(),
                request.priority(), request.stackable(), request.cap(), request.toDiscountBase());

        Discount discount = createDiscountUseCase.createDiscount(command);

        return ResponseEntity.status(HttpStatus.CREATED).body(DiscountResponse.from(discount));
    }

    @GetMapping
    public ResponseEntity<List<DiscountResponse>> listByTenant(@PathVariable String tenantId) {
        List<DiscountResponse> discounts = discountQueryUseCase.listByTenant(tenantId).stream()
                .map(DiscountResponse::from)
                .toList();

        return ResponseEntity.ok(discounts);
    }

    @GetMapping("/{discountId}")
    public ResponseEntity<DiscountResponse> getById(@PathVariable String tenantId, @PathVariable UUID discountId) {
        return ResponseEntity.ok(DiscountResponse.from(discountQueryUseCase.getById(tenantId, discountId)));
    }

    @ExceptionHandler({InvalidDiscountException.class, IllegalArgumentException.class})
    public ResponseEntity<ErrorResponse> handleValidationError(RuntimeException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse("validation_error", ex.getMessage()));
    }

    @ExceptionHandler({TenantNotFoundException.class, CatalogItemNotFoundException.class, DiscountNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("not_found", ex.getMessage()));
    }

    @ExceptionHandler(TenantInactiveException.class)
    public ResponseEntity<ErrorResponse> handleTenantInactive(TenantInactiveException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse("tenant_inactive", ex.getMessage()));
    }
}
