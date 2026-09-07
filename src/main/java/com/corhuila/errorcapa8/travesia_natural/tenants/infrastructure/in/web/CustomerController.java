package com.corhuila.errorcapa8.travesia_natural.tenants.infrastructure.in.web;

import com.corhuila.errorcapa8.travesia_natural.common.web.dto.ErrorResponse;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.EmailAlreadyRegisteredException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.InvalidTenantException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantInactiveException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.Membership;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.in.RegisterCustomerCommand;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.in.RegisterCustomerUseCase;
import com.corhuila.errorcapa8.travesia_natural.tenants.infrastructure.in.web.dto.CustomerResponse;
import com.corhuila.errorcapa8.travesia_natural.tenants.infrastructure.in.web.dto.RegisterCustomerRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping("/api/tenants/{tenantId}/customers")
public class CustomerController {

    private final RegisterCustomerUseCase registerCustomerUseCase;

    public CustomerController(RegisterCustomerUseCase registerCustomerUseCase) {
        this.registerCustomerUseCase = registerCustomerUseCase;
    }

    @PostMapping
    public ResponseEntity<CustomerResponse> register(@PathVariable String tenantId,
                                                       @RequestBody RegisterCustomerRequest request) {
        if (!Objects.equals(request.password(), request.passwordConfirmation())) {
            throw new InvalidTenantException("password and passwordConfirmation must match");
        }

        RegisterCustomerCommand command = new RegisterCustomerCommand(
                tenantId, request.firstName(), request.lastName(), request.email(), request.phone(),
                request.password());

        Membership customer = registerCustomerUseCase.registerCustomer(command);

        return ResponseEntity.status(HttpStatus.CREATED).body(CustomerResponse.from(customer));
    }

    @ExceptionHandler({InvalidTenantException.class, IllegalArgumentException.class})
    public ResponseEntity<ErrorResponse> handleValidationError(RuntimeException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse("validation_error", ex.getMessage()));
    }

    @ExceptionHandler(TenantNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(TenantNotFoundException ex) {
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
}
