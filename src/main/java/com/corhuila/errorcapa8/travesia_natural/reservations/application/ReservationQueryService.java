package com.corhuila.errorcapa8.travesia_natural.reservations.application;

import com.corhuila.errorcapa8.travesia_natural.reservations.domain.exception.ReservationNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.Reservation;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.in.ReservationQueryUseCase;
import com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.out.ReservationRepositoryPort;
import com.corhuila.errorcapa8.travesia_natural.tenants.domain.exception.TenantNotFoundException;
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

    private void requireTenant(String tenantId) {
        if (!tenantRepositoryPort.existsById(tenantId)) {
            throw new TenantNotFoundException(tenantId);
        }
    }
}
