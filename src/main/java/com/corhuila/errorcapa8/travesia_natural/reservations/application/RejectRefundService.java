package com.corhuila.errorcapa8.travesia_natural.reservations.application;

import com.corhuila.errorcapa8.travesia_natural.reservations.domain.exception.ReservationNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.Reservation;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.in.RejectRefundCommand;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.in.RejectRefundUseCase;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.out.ReservationRepositoryPort;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantInactiveException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.Tenant;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.TenantStatus;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.out.MembershipRepositoryPort;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.out.TenantRepositoryPort;
import org.springframework.stereotype.Service;

@Service
public class RejectRefundService implements RejectRefundUseCase {

    private final TenantRepositoryPort tenantRepositoryPort;
    private final MembershipRepositoryPort membershipRepositoryPort;
    private final ReservationRepositoryPort reservationRepositoryPort;

    public RejectRefundService(TenantRepositoryPort tenantRepositoryPort,
                                MembershipRepositoryPort membershipRepositoryPort,
                                ReservationRepositoryPort reservationRepositoryPort) {
        this.tenantRepositoryPort = tenantRepositoryPort;
        this.membershipRepositoryPort = membershipRepositoryPort;
        this.reservationRepositoryPort = reservationRepositoryPort;
    }

    @Override
    public Reservation rejectRefund(RejectRefundCommand command) {
        requireActiveTenant(command.tenantId());
        RefundActorValidator.requireAdministratorActor(membershipRepositoryPort, command.tenantId(),
                command.actorId());

        Reservation reservation = reservationRepositoryPort
                .findByTenantIdAndReservationId(command.tenantId(), command.reservationId())
                .orElseThrow(() -> new ReservationNotFoundException(command.reservationId().toString()));

        Reservation rejected = reservation.rejectRefund(command.actorId(), command.reason());

        return reservationRepositoryPort.save(rejected);
    }

    private void requireActiveTenant(String tenantId) {
        Tenant tenant = tenantRepositoryPort.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));

        if (tenant.tenantStatus() == TenantStatus.INACTIVO) {
            throw new TenantInactiveException(tenant.tenantId());
        }
    }
}
