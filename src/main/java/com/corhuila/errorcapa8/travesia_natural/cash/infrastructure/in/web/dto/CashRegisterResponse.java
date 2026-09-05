package com.corhuila.errorcapa8.travesia_natural.cash.infrastructure.in.web.dto;

import com.corhuila.errorcapa8.travesia_natural.cash.domain.model.CashCorrection;
import com.corhuila.errorcapa8.travesia_natural.cash.domain.model.CashRegister;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CashRegisterResponse(UUID cashRegisterId, LocalDate businessDate, BigDecimal baseAmount,
                                    String status, List<CashMovementResponse> movements,
                                    List<CashCorrection> corrections, String closedBy, Instant closedAt,
                                    BigDecimal totalAmount) {

    public static CashRegisterResponse from(CashRegister cashRegister) {
        return new CashRegisterResponse(cashRegister.cashRegisterId(), cashRegister.businessDate(),
                cashRegister.baseAmount(), cashRegister.status().name(),
                cashRegister.movements().stream().map(CashMovementResponse::from).toList(),
                cashRegister.corrections(), cashRegister.closedBy(), cashRegister.closedAt(),
                cashRegister.totalAmount());
    }
}
