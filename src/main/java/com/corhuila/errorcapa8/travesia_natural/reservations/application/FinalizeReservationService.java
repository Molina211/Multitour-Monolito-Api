package com.corhuila.errorcapa8.travesia_natural.reservations.application;

import com.corhuila.errorcapa8.travesia_natural.reservations.domain.exception.ReservationNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.Reservation;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.in.FinalizeReservationCommand;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.in.FinalizeReservationUseCase;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.out.ReservationRepositoryPort;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantInactiveException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.Tenant;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.TenantStatus;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.out.TenantRepositoryPort;
import org.springframework.stereotype.Service;

@Service
public class FinalizeReservationService implements FinalizeReservationUseCase {

    private final TenantRepositoryPort tenantRepositoryPort;
    private final ReservationRepositoryPort reservationRepositoryPort;

    public FinalizeReservationService(TenantRepositoryPort tenantRepositoryPort,
                                       ReservationRepositoryPort reservationRepositoryPort) {
        this.tenantRepositoryPort = tenantRepositoryPort;
        this.reservationRepositoryPort = reservationRepositoryPort;
    }

    @Override
    public Reservation finalizeReservation(FinalizeReservationCommand command) {
        requireActiveTenant(command.tenantId());

        Reservation reservation = reservationRepositoryPort
                .findByTenantIdAndReservationId(command.tenantId(), command.reservationId())
                .orElseThrow(() -> new ReservationNotFoundException(command.reservationId().toString()));

        Reservation finalized = reservation.finalizeExecution(command.actorId());

        return reservationRepositoryPort.save(finalized);
    }

    private void requireActiveTenant(String tenantId) {
        Tenant tenant = tenantRepositoryPort.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));

        if (tenant.tenantStatus() == TenantStatus.INACTIVO) {
            throw new TenantInactiveException(tenant.tenantId());
        }
    }
}
