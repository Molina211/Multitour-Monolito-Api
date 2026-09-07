package com.corhuila.errorcapa8.travesia_natural.cash.application;

import com.corhuila.errorcapa8.travesia_natural.cash.domain.exception.CashRegisterNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.cash.domain.model.CashRegister;
import com.corhuila.errorcapa8.travesia_natural.cash.domain.port.in.AddCashCorrectionCommand;
import com.corhuila.errorcapa8.travesia_natural.cash.domain.port.in.AddCashCorrectionUseCase;
import com.corhuila.errorcapa8.travesia_natural.cash.domain.port.out.CashRegisterRepositoryPort;
import org.springframework.stereotype.Service;

@Service
public class AddCashCorrectionService implements AddCashCorrectionUseCase {

    private final TenantGuard tenantGuard;
    private final CashRegisterRepositoryPort cashRegisterRepositoryPort;

    public AddCashCorrectionService(TenantGuard tenantGuard, CashRegisterRepositoryPort cashRegisterRepositoryPort) {
        this.tenantGuard = tenantGuard;
        this.cashRegisterRepositoryPort = cashRegisterRepositoryPort;
    }

    @Override
    public CashRegister addCorrection(AddCashCorrectionCommand command) {
        tenantGuard.requireActive(command.tenantId());

        CashRegister cashRegister = cashRegisterRepositoryPort
                .findByTenantIdAndCashRegisterId(command.tenantId(), command.cashRegisterId())
                .orElseThrow(() -> new CashRegisterNotFoundException(
                        "cash register not found: " + command.cashRegisterId()));

        CashRegister updated = cashRegister.addCorrection(command.justification(), command.actorId());

        return cashRegisterRepositoryPort.save(updated);
    }
}
