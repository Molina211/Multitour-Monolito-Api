package com.corhuila.errorcapa8.travesia_natural.cash.domain.port.in;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OpenCashRegisterCommand(String tenantId, LocalDate businessDate, BigDecimal baseAmount) {
}
