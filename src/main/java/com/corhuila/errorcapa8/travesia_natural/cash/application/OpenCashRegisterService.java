package com.corhuila.errorcapa8.travesia_natural.cash.application;

import com.corhuila.errorcapa8.travesia_natural.cash.domain.exception.CashRegisterAlreadyOpenException;
import com.corhuila.errorcapa8.travesia_natural.cash.domain.model.CashRegister;
import com.corhuila.errorcapa8.travesia_natural.cash.domain.port.in.OpenCashRegisterCommand;
import com.corhuila.errorcapa8.travesia_natural.cash.domain.port.in.OpenCashRegisterUseCase;
import com.corhuila.errorcapa8.travesia_natural.cash.domain.port.out.CashRegisterRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class OpenCashRegisterService implements OpenCashRegisterUseCase {

    private final TenantGuard tenantGuard;
    private final CashRegisterRepositoryPort cashRegisterRepositoryPort;

    public OpenCashRegisterService(TenantGuard tenantGuard, CashRegisterRepositoryPort cashRegisterRepositoryPort) {
        this.tenantGuard = tenantGuard;
        this.cashRegisterRepositoryPort = cashRegisterRepositoryPort;
    }

    @Override
    public CashRegister openCashRegister(OpenCashRegisterCommand command) {
        tenantGuard.requireActive(command.tenantId());

        cashRegisterRepositoryPort.findByTenantIdAndBusinessDate(command.tenantId(), command.businessDate())
                .ifPresent(existing -> {
                    throw new CashRegisterAlreadyOpenException(
                            "a cash register already exists for tenant " + command.tenantId() + " on "
                                    + command.businessDate());
                });

        CashRegister cashRegister = CashRegister.open(UUID.randomUUID(), command.tenantId(), command.businessDate(),
                command.baseAmount());

        return cashRegisterRepositoryPort.save(cashRegister);
    }
}
