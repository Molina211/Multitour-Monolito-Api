package com.corhuila.errorcapa8.travesia_natural.operations.application;

import com.corhuila.errorcapa8.travesia_natural.operations.domain.exception.ExecutionNotStartedException;
import com.corhuila.errorcapa8.travesia_natural.operations.domain.model.OperationCost;
import com.corhuila.errorcapa8.travesia_natural.operations.domain.port.in.RegisterOperationCostCommand;
import com.corhuila.errorcapa8.travesia_natural.operations.domain.port.in.RegisterOperationCostUseCase;
import com.corhuila.errorcapa8.travesia_natural.operations.domain.port.out.OperationCostRepositoryPort;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.exception.ReservationNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.Reservation;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.ReservationStatus;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.out.ReservationRepositoryPort;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantInactiveException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.Tenant;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.TenantStatus;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.out.TenantRepositoryPort;
import org.springframework.stereotype.Service;

@Service
public class RegisterOperationCostService implements RegisterOperationCostUseCase {

    private final TenantRepositoryPort tenantRepositoryPort;
    private final ReservationRepositoryPort reservationRepositoryPort;
    private final OperationCostRepositoryPort operationCostRepositoryPort;

    public RegisterOperationCostService(TenantRepositoryPort tenantRepositoryPort,
                                         ReservationRepositoryPort reservationRepositoryPort,
                                         OperationCostRepositoryPort operationCostRepositoryPort) {
        this.tenantRepositoryPort = tenantRepositoryPort;
        this.reservationRepositoryPort = reservationRepositoryPort;
        this.operationCostRepositoryPort = operationCostRepositoryPort;
    }

    @Override
    public OperationCost registerCost(RegisterOperationCostCommand command) {
        requireActiveTenant(command.tenantId());

        Reservation reservation = reservationRepositoryPort
                .findByTenantIdAndReservationId(command.tenantId(), command.reservationId())
                .orElseThrow(() -> new ReservationNotFoundException(command.reservationId().toString()));

        if (reservation.reservationStatus() != ReservationStatus.EN_EJECUCION) {
            throw new ExecutionNotStartedException(
                    "reservation execution has not started, current status: "
                            + reservation.reservationStatus().label() + " (reservation: " + command.reservationId()
                            + ")");
        }

        OperationCost operationCost = OperationCost.create(command.tenantId(), command.reservationId(),
                command.concept(), command.amount(), command.actorId());

        return operationCostRepositoryPort.save(operationCost);
    }

    private void requireActiveTenant(String tenantId) {
        Tenant tenant = tenantRepositoryPort.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));

        if (tenant.tenantStatus() == TenantStatus.INACTIVO) {
            throw new TenantInactiveException(tenant.tenantId());
        }
    }
}
