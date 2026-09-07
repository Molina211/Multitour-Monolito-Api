package com.corhuila.errorcapa8.travesia_natural.reservations.infrastructure.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservationJpaRepository extends JpaRepository<ReservationEntity, UUID> {

    Optional<ReservationEntity> findByTenantIdAndReservationId(String tenantId, UUID reservationId);

    List<ReservationEntity> findAllByTenantId(String tenantId);

    List<ReservationEntity> findAllByTenantIdAndCustomerId(String tenantId, String customerId);
}
