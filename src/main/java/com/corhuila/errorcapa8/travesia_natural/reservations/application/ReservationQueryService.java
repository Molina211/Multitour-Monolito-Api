package com.corhuila.errorcapa8.travesia_natural.reservations.application;

import com.corhuila.errorcapa8.travesia_natural.reservations.domain.exception.ReservationNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.PaymentStatus;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.Reservation;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.ReservationStatus;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.in.ReservationQueryUseCase;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.out.ReservationRepositoryPort;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantInactiveException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.Tenant;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.model.TenantStatus;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.port.out.TenantRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ReservationQueryService implements ReservationQueryUseCase {

    private final TenantRepositoryPort tenantRepositoryPort;
    private final ReservationRepositoryPort reservationRepositoryPort;

    public ReservationQueryService(TenantRepositoryPort tenantRepositoryPort,
                                    ReservationRepositoryPort reservationRepositoryPort) {
        this.tenantRepositoryPort = tenantRepositoryPort;
        this.reservationRepositoryPort = reservationRepositoryPort;
    }

    @Override
    public Reservation getById(String tenantId, UUID reservationId) {
        requireTenant(tenantId);

        return reservationRepositoryPort.findByTenantIdAndReservationId(tenantId, reservationId)
                .orElseThrow(() -> new ReservationNotFoundException(reservationId.toString()));
    }

    @Override
    public List<Reservation> listByTenant(String tenantId) {
        requireTenant(tenantId);

        return reservationRepositoryPort.findAllByTenantId(tenantId);
    }

    @Override
    public List<Reservation> listByTenantAndCustomer(String tenantId, String customerId) {
        requireTenant(tenantId);

        return reservationRepositoryPort.findAllByTenantIdAndCustomerId(tenantId, customerId);
    }

    /**
     * Same {@link ReservationNotFoundException} for "does not exist" and "exists but
     * belongs to someone else" — mirrors CollaboratorQueryService (spec 014): never reveal
     * a resource's existence to a caller who is not its owner.
     */
    @Override
    public Reservation getByIdForCustomer(String tenantId, String customerId, UUID reservationId) {
        requireTenant(tenantId);

        Reservation reservation = reservationRepositoryPort.findByTenantIdAndReservationId(tenantId, reservationId)
                .orElseThrow(() -> new ReservationNotFoundException(reservationId.toString()));

        if (!reservation.customerId().equals(customerId)) {
            throw new ReservationNotFoundException(reservationId.toString());
        }

        return reservation;
    }

    @Override
    public List<Reservation> listPendingSupportByTenant(String tenantId) {
        requireActiveTenant(tenantId);

        return reservationRepositoryPort.findAllByTenantId(tenantId).stream()
                .filter(reservation -> reservation.paymentStatus() == PaymentStatus.EN_VALIDACION)
                .toList();
    }

    @Override
    public List<Reservation> listPendingExecutionByTenant(String tenantId) {
        requireActiveTenant(tenantId);

        return reservationRepositoryPort.findAllByTenantId(tenantId).stream()
                .filter(reservation -> reservation.reservationStatus() == ReservationStatus.CONFIRMADA)
                .toList();
    }

    private void requireTenant(String tenantId) {
        if (!tenantRepositoryPort.existsById(tenantId)) {
            throw new TenantNotFoundException(tenantId);
        }
    }

    private void requireActiveTenant(String tenantId) {
        Tenant tenant = tenantRepositoryPort.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));

        if (tenant.tenantStatus() == TenantStatus.INACTIVO) {
            throw new TenantInactiveException(tenant.tenantId());
        }
    }
}
