package com.corhuila.errorcapa8.travesia_natural.operations.application;

import com.corhuila.errorcapa8.travesia_natural.operations.domain.model.Execution;
import com.corhuila.errorcapa8.travesia_natural.operations.domain.port.in.RegisterExecutionCommand;
import com.corhuila.errorcapa8.travesia_natural.operations.domain.port.in.RegisterExecutionUseCase;
import com.corhuila.errorcapa8.travesia_natural.operations.domain.port.out.ExecutionRepositoryPort;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.exception.ReservationNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.Reservation;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.out.ReservationRepositoryPort;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantInactiveException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.Tenant;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.TenantStatus;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.out.TenantRepositoryPort;
import org.springframework.stereotype.Service;

@Service
public class RegisterExecutionService implements RegisterExecutionUseCase {

    private final TenantRepositoryPort tenantRepositoryPort;
    private final ReservationRepositoryPort reservationRepositoryPort;
    private final ExecutionRepositoryPort executionRepositoryPort;

    public RegisterExecutionService(TenantRepositoryPort tenantRepositoryPort,
                                     ReservationRepositoryPort reservationRepositoryPort,
                                     ExecutionRepositoryPort executionRepositoryPort) {
        this.tenantRepositoryPort = tenantRepositoryPort;
        this.reservationRepositoryPort = reservationRepositoryPort;
        this.executionRepositoryPort = executionRepositoryPort;
    }

    @Override
    public Execution registerExecution(RegisterExecutionCommand command) {
        requireActiveTenant(command.tenantId());

        Reservation reservation = reservationRepositoryPort
                .findByTenantIdAndReservationId(command.tenantId(), command.reservationId())
                .orElseThrow(() -> new ReservationNotFoundException(command.reservationId().toString()));

        Reservation executingReservation = reservation.startExecution();
        reservationRepositoryPort.save(executingReservation);

        Execution execution = Execution.create(command.tenantId(), command.reservationId(), command.served(),
                command.executed(), command.causal(), command.actorId());

        return executionRepositoryPort.save(execution);
    }

    private void requireActiveTenant(String tenantId) {
        Tenant tenant = tenantRepositoryPort.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));

        if (tenant.tenantStatus() == TenantStatus.INACTIVO) {
            throw new TenantInactiveException(tenant.tenantId());
        }
    }
}
