package com.corhuila.errorcapa8.travesia_natural.reservations.application;

import com.corhuila.errorcapa8.travesia_natural.reservations.domain.exception.ReservationNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.Reservation;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.in.RefundReservationCommand;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.in.RefundReservationUseCase;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.out.ReservationRepositoryPort;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantInactiveException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.Tenant;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.TenantStatus;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.out.TenantRepositoryPort;
import org.springframework.stereotype.Service;

@Service
public class RefundReservationService implements RefundReservationUseCase {

    private final TenantRepositoryPort tenantRepositoryPort;
    private final ReservationRepositoryPort reservationRepositoryPort;

    public RefundReservationService(TenantRepositoryPort tenantRepositoryPort,
                                     ReservationRepositoryPort reservationRepositoryPort) {
        this.tenantRepositoryPort = tenantRepositoryPort;
        this.reservationRepositoryPort = reservationRepositoryPort;
    }

    @Override
    public Reservation refundReservation(RefundReservationCommand command) {
        requireActiveTenant(command.tenantId());

        Reservation reservation = reservationRepositoryPort
                .findByTenantIdAndReservationId(command.tenantId(), command.reservationId())
                .orElseThrow(() -> new ReservationNotFoundException(command.reservationId().toString()));

        Reservation refunded = reservation.refund(command.amount(), command.reason(), command.actorId(),
                command.method());

        return reservationRepositoryPort.save(refunded);
    }

    private void requireActiveTenant(String tenantId) {
        Tenant tenant = tenantRepositoryPort.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));

        if (tenant.tenantStatus() == TenantStatus.INACTIVO) {
            throw new TenantInactiveException(tenant.tenantId());
        }
    }
}
