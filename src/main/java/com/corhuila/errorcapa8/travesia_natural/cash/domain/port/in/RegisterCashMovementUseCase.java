package com.corhuila.errorcapa8.travesia_natural.cash.domain.port.in;

import com.corhuila.errorcapa8.travesia_natural.cash.domain.model.CashRegister;

public interface RegisterCashMovementUseCase {

    CashRegister registerMovement(RegisterCashMovementCommand command);
}
