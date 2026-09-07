package com.corhuila.errorcapa8.travesia_natural.cash.domain.port.in;

import com.corhuila.errorcapa8.travesia_natural.cash.domain.model.CashRegister;

public interface AddCashCorrectionUseCase {

    CashRegister addCorrection(AddCashCorrectionCommand command);
}
