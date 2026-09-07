package com.corhuila.errorcapa8.travesia_natural.cash.application;

import com.corhuila.errorcapa8.travesia_natural.cash.domain.exception.CashRegisterNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.cash.domain.model.CashRegister;
import com.corhuila.errorcapa8.travesia_natural.cash.domain.model.CashRegisterStatus;
import com.corhuila.errorcapa8.travesia_natural.cash.domain.port.in.CashRegisterQueryUseCase;
import com.corhuila.errorcapa8.travesia_natural.cash.domain.port.out.CashRegisterRepositoryPort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class CashRegisterQueryService implements CashRegisterQueryUseCase {

    private final TenantGuard tenantGuard;
    private final CashRegisterRepositoryPort cashRegisterRepositoryPort;
    private final RefundsTotalCalculator refundsTotalCalculator;

    public CashRegisterQueryService(TenantGuard tenantGuard, CashRegisterRepositoryPort cashRegisterRepositoryPort,
                                     RefundsTotalCalculator refundsTotalCalculator) {
        this.tenantGuard = tenantGuard;
        this.cashRegisterRepositoryPort = cashRegisterRepositoryPort;
        this.refundsTotalCalculator = refundsTotalCalculator;
    }

    @Override
    public CashRegister getByBusinessDate(String tenantId, LocalDate businessDate) {
        tenantGuard.requireActive(tenantId);

        CashRegister cashRegister = cashRegisterRepositoryPort
                .findByTenantIdAndBusinessDate(tenantId, businessDate)
                .orElseThrow(() -> new CashRegisterNotFoundException(
                        "no cash register found for tenant " + tenantId + " on " + businessDate));

        return withLiveTotalIfOpen(cashRegister);
    }

    @Override
    public List<CashRegister> listHistory(String tenantId) {
        tenantGuard.requireActive(tenantId);

        return cashRegisterRepositoryPort.findAllClosedByTenantId(tenantId);
    }

    /**
     * Una caja `ABIERTA` no tiene `totalAmount` persistido (se congela recién al cerrar):
     * para la consulta, se muestra el total en vivo sin persistir nada (spec 013, `GET
     * /?businessDate`).
     */
    private CashRegister withLiveTotalIfOpen(CashRegister cashRegister) {
        if (cashRegister.status() != CashRegisterStatus.ABIERTA) {
            return cashRegister;
        }
        BigDecimal refundsTotal = refundsTotalCalculator.totalForBusinessDate(cashRegister.tenantId(),
                cashRegister.businessDate());
        BigDecimal liveTotal = cashRegister.computeTotal(refundsTotal);

        return CashRegister.reconstitute(cashRegister.cashRegisterId(), cashRegister.tenantId(),
                cashRegister.businessDate(), cashRegister.baseAmount(), cashRegister.status(),
                cashRegister.movements(), cashRegister.corrections(), cashRegister.closedBy(),
                cashRegister.closedAt(), liveTotal);
    }
}
