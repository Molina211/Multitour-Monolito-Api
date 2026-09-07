package com.corhuila.errorcapa8.travesia_natural.cash.application;

import com.corhuila.errorcapa8.travesia_natural.cash.domain.exception.CashRegisterNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.cash.domain.model.CashRegister;
import com.corhuila.errorcapa8.travesia_natural.cash.domain.port.in.CloseCashRegisterCommand;
import com.corhuila.errorcapa8.travesia_natural.cash.domain.port.in.CloseCashRegisterUseCase;
import com.corhuila.errorcapa8.travesia_natural.cash.domain.port.out.CashRegisterRepositoryPort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class CloseCashRegisterService implements CloseCashRegisterUseCase {

    private final TenantGuard tenantGuard;
    private final CashRegisterRepositoryPort cashRegisterRepositoryPort;
    private final RefundsTotalCalculator refundsTotalCalculator;

    public CloseCashRegisterService(TenantGuard tenantGuard, CashRegisterRepositoryPort cashRegisterRepositoryPort,
                                     RefundsTotalCalculator refundsTotalCalculator) {
        this.tenantGuard = tenantGuard;
        this.cashRegisterRepositoryPort = cashRegisterRepositoryPort;
        this.refundsTotalCalculator = refundsTotalCalculator;
    }

    @Override
    public CashRegister closeCashRegister(CloseCashRegisterCommand command) {
        tenantGuard.requireActive(command.tenantId());

        CashRegister cashRegister = cashRegisterRepositoryPort
                .findByTenantIdAndCashRegisterId(command.tenantId(), command.cashRegisterId())
                .orElseThrow(() -> new CashRegisterNotFoundException(
                        "cash register not found: " + command.cashRegisterId()));

        BigDecimal refundsTotal = refundsTotalCalculator.totalForBusinessDate(command.tenantId(),
                cashRegister.businessDate());

        CashRegister closed = cashRegister.close(command.actorId(), refundsTotal);

        return cashRegisterRepositoryPort.save(closed);
    }
}
