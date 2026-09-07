package com.corhuila.errorcapa8.travesia_natural.cash.application;

import com.corhuila.errorcapa8.travesia_natural.cash.domain.exception.CashRegisterNotFoundException;
import com.corhuila.errorcapa8.travesia_natural.cash.domain.model.CashRegister;
import com.corhuila.errorcapa8.travesia_natural.cash.domain.port.in.RegisterCashMovementCommand;
import com.corhuila.errorcapa8.travesia_natural.cash.domain.port.in.RegisterCashMovementUseCase;
import com.corhuila.errorcapa8.travesia_natural.cash.domain.port.out.CashRegisterRepositoryPort;
import org.springframework.stereotype.Service;

@Service
public class RegisterCashMovementService implements RegisterCashMovementUseCase {

    private final TenantGuard tenantGuard;
    private final CashRegisterRepositoryPort cashRegisterRepositoryPort;

    public RegisterCashMovementService(TenantGuard tenantGuard,
                                        CashRegisterRepositoryPort cashRegisterRepositoryPort) {
        this.tenantGuard = tenantGuard;
        this.cashRegisterRepositoryPort = cashRegisterRepositoryPort;
    }

    @Override
    public CashRegister registerMovement(RegisterCashMovementCommand command) {
        tenantGuard.requireActive(command.tenantId());

        CashRegister cashRegister = cashRegisterRepositoryPort
                .findByTenantIdAndCashRegisterId(command.tenantId(), command.cashRegisterId())
                .orElseThrow(() -> new CashRegisterNotFoundException(
                        "cash register not found: " + command.cashRegisterId()));

        CashRegister updated = cashRegister.registerMovement(command.type(), command.amount(), command.concept(),
                command.actorId());

        return cashRegisterRepositoryPort.save(updated);
    }
}
