package com.corhuila.errorcapa8.travesia_natural.cash.infrastructure.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CashRegisterJpaRepository extends JpaRepository<CashRegisterEntity, UUID> {

    Optional<CashRegisterEntity> findByTenantIdAndCashRegisterId(String tenantId, UUID cashRegisterId);

    Optional<CashRegisterEntity> findByTenantIdAndBusinessDate(String tenantId, LocalDate businessDate);

    List<CashRegisterEntity> findAllByTenantIdAndStatus(String tenantId, String status);
}
