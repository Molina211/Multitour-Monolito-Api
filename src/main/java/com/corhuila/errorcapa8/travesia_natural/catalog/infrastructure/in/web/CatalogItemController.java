package com.corhuila.errorcapa8.travesia_natural.catalog.infrastructure.in.web;

import com.corhuila.errorcapa8.travesia_natural.catalog.domain.exception.CatalogItemNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.catalog.domain.exception.InvalidCatalogItemException;
import com.corhuila.errorcapa8.travesia_natural.catalog.domain.model.CatalogItem;
import com.corhuila.errorcapa8.travesia_natural.catalog.domain.port.in.CatalogItemQueryUseCase;
import com.corhuila.errorcapa8.travesia_natural.catalog.domain.port.in.CreateCatalogItemCommand;
import com.corhuila.errorcapa8.travesia_natural.catalog.domain.port.in.CreateCatalogItemUseCase;
import com.corhuila.errorcapa8.travesia_natural.catalog.domain.port.in.DeactivateCatalogItemUseCase;
import com.corhuila.errorcapa8.travesia_natural.catalog.domain.port.in.ReactivateCatalogItemUseCase;
import com.corhuila.errorcapa8.travesia_natural.catalog.domain.port.in.UpdateCatalogItemCommand;
import com.corhuila.errorcapa8.travesia_natural.catalog.domain.port.in.UpdateCatalogItemUseCase;
import com.corhuila.errorcapa8.travesia_natural.catalog.infrastructure.in.web.dto.CatalogItemPatchRequest;
import com.corhuila.errorcapa8.travesia_natural.catalog.infrastructure.in.web.dto.CatalogItemRequest;
import com.corhuila.errorcapa8.travesia_natural.catalog.infrastructure.in.web.dto.CatalogItemResponse;
import com.corhuila.errorcapa8.travesia_natural.common.web.dto.ErrorResponse;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantInactiveException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tenants/{tenantId}/catalog-items")
public class CatalogItemController {

    private final CreateCatalogItemUseCase createCatalogItemUseCase;
    private final UpdateCatalogItemUseCase updateCatalogItemUseCase;
    private final DeactivateCatalogItemUseCase deactivateCatalogItemUseCase;
    private final ReactivateCatalogItemUseCase reactivateCatalogItemUseCase;
    private final CatalogItemQueryUseCase catalogItemQueryUseCase;

    public CatalogItemController(CreateCatalogItemUseCase createCatalogItemUseCase,
                                  UpdateCatalogItemUseCase updateCatalogItemUseCase,
                                  DeactivateCatalogItemUseCase deactivateCatalogItemUseCase,
                                  ReactivateCatalogItemUseCase reactivateCatalogItemUseCase,
                                  CatalogItemQueryUseCase catalogItemQueryUseCase) {
        this.createCatalogItemUseCase = createCatalogItemUseCase;
        this.updateCatalogItemUseCase = updateCatalogItemUseCase;
        this.deactivateCatalogItemUseCase = deactivateCatalogItemUseCase;
        this.reactivateCatalogItemUseCase = reactivateCatalogItemUseCase;
        this.catalogItemQueryUseCase = catalogItemQueryUseCase;
    }

    @PostMapping
    public ResponseEntity<CatalogItemResponse> create(@PathVariable String tenantId,
                                                        @RequestBody CatalogItemRequest request) {
        CreateCatalogItemCommand command = new CreateCatalogItemCommand(
                tenantId, request.type(), request.name(), request.price(), request.capacity(),
                request.restrictions(), request.validFrom(), request.validTo(), request.policy(), request.image(),
                request.route(), request.operationalCost());

        CatalogItem catalogItem = createCatalogItemUseCase.createCatalogItem(command);

        return ResponseEntity.status(HttpStatus.CREATED).body(CatalogItemResponse.from(catalogItem));
    }

    @GetMapping
    public ResponseEntity<List<CatalogItemResponse>> listByTenant(@PathVariable String tenantId) {
        List<CatalogItemResponse> items = catalogItemQueryUseCase.listByTenant(tenantId).stream()
                .map(CatalogItemResponse::from)
                .toList();

        return ResponseEntity.ok(items);
    }

    @GetMapping("/{itemId}")
    public ResponseEntity<CatalogItemResponse> getById(@PathVariable String tenantId, @PathVariable UUID itemId) {
        return ResponseEntity.ok(CatalogItemResponse.from(catalogItemQueryUseCase.getById(tenantId, itemId)));
    }

    @PatchMapping("/{itemId}")
    public ResponseEntity<CatalogItemResponse> update(@PathVariable String tenantId, @PathVariable UUID itemId,
                                                        @RequestBody CatalogItemPatchRequest request) {
        UpdateCatalogItemCommand command = new UpdateCatalogItemCommand(
                tenantId, itemId, request.name(), request.price(), request.capacity(), request.restrictions(),
                request.validFrom(), request.validTo(), request.policy(), request.image(), request.route(),
                request.operationalCost());

        CatalogItem catalogItem = updateCatalogItemUseCase.updateCatalogItem(command);

        return ResponseEntity.ok(CatalogItemResponse.from(catalogItem));
    }

    @PostMapping("/{itemId}/deactivate")
    public ResponseEntity<CatalogItemResponse> deactivate(@PathVariable String tenantId, @PathVariable UUID itemId) {
        CatalogItem catalogItem = deactivateCatalogItemUseCase.deactivateCatalogItem(tenantId, itemId);

        return ResponseEntity.ok(CatalogItemResponse.from(catalogItem));
    }

    @PostMapping("/{itemId}/reactivate")
    public ResponseEntity<CatalogItemResponse> reactivate(@PathVariable String tenantId, @PathVariable UUID itemId) {
        CatalogItem catalogItem = reactivateCatalogItemUseCase.reactivateCatalogItem(tenantId, itemId);

        return ResponseEntity.ok(CatalogItemResponse.from(catalogItem));
    }

    @ExceptionHandler({InvalidCatalogItemException.class, IllegalArgumentException.class})
    public ResponseEntity<ErrorResponse> handleValidationError(RuntimeException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse("validation_error", ex.getMessage()));
    }

    @ExceptionHandler({TenantNotFoundException.class, CatalogItemNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("not_found", ex.getMessage()));
    }

    @ExceptionHandler(TenantInactiveException.class)
    public ResponseEntity<ErrorResponse> handleTenantInactive(TenantInactiveException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse("tenant_inactive", ex.getMessage()));
    }
}
