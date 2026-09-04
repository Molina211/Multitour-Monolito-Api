package com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.in;

import com.corhuila.errorcapa8.travesia_natural.reservations.domain.model.Reservation;

import java.util.List;
import java.util.UUID;

public interface ReservationQueryUseCase {

    Reservation getById(String tenantId, UUID reservationId);

    List<Reservation> listByTenant(String tenantId);

    List<Reservation> listPendingSupportByTenant(String tenantId);

    List<Reservation> listPendingExecutionByTenant(String tenantId);
}
