package com.corhuila.errorcapa8.travesia_natural.cash.domain.port.in;

import com.corhuila.errorcapa8.travesia_natural.cash.domain.model.CashRegister;

import java.time.LocalDate;
import java.util.List;

public interface CashRegisterQueryUseCase {

    CashRegister getByBusinessDate(String tenantId, LocalDate businessDate);

    List<CashRegister> listHistory(String tenantId);
}
