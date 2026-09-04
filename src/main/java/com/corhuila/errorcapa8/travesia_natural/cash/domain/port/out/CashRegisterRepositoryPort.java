package com.corhuila.errorcapa8.travesia_natural.cash.domain.port.out;

import com.corhuila.errorcapa8.travesia_natural.cash.domain.model.CashRegister;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CashRegisterRepositoryPort {

    CashRegister save(CashRegister cashRegister);

    Optional<CashRegister> findByTenantIdAndCashRegisterId(String tenantId, UUID cashRegisterId);

    Optional<CashRegister> findByTenantIdAndBusinessDate(String tenantId, LocalDate businessDate);

    /**
     * Todas las cajas `CERRADA`s de un tenant, sin filtrar por periodo — el histórico
     * (spec 013) y la consolidación mensual (RF-012) filtran/agrupan sobre el mismo
     * resultado en la capa de aplicación, igual que {@code findAllByTenantId} en
     * {@code reservations}/{@code operations}.
     */
    List<CashRegister> findAllClosedByTenantId(String tenantId);
}
