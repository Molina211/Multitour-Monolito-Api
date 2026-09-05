package com.corhuila.errorcapa8.travesia_natural.reservations.application;

import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.Reservation;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.in.CreateReservationCommand;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.in.CreateReservationUseCase;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.out.ReservationRepositoryPort;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantInactiveException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.Tenant;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.TenantStatus;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.out.TenantRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CreateReservationService implements CreateReservationUseCase {

    private final TenantRepositoryPort tenantRepositoryPort;
    private final ReservationRepositoryPort reservationRepositoryPort;

    public CreateReservationService(TenantRepositoryPort tenantRepositoryPort,
                                     ReservationRepositoryPort reservationRepositoryPort) {
        this.tenantRepositoryPort = tenantRepositoryPort;
        this.reservationRepositoryPort = reservationRepositoryPort;
    }

    @Override
    public Reservation createReservation(CreateReservationCommand command) {
        Tenant tenant = tenantRepositoryPort.findById(command.tenantId())
                .orElseThrow(() -> new TenantNotFoundException(command.tenantId()));

        if (tenant.tenantStatus() == TenantStatus.INACTIVO) {
            throw new TenantInactiveException(tenant.tenantId());
        }

        Reservation reservation = Reservation.create(
                UUID.randomUUID(),
                tenant.tenantId(),
                command.customerId(),
                command.projectedValue(),
                command.reservedServices(),
                command.holderDocument(),
                command.companions());

        return reservationRepositoryPort.save(reservation);
    }
}
