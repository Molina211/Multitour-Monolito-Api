package com.corhuila.errorcapa8.travesia_natural.cash.infrastructure.in.web.dto;

import com.corhuila.errorcapa8.travesia_natural.cash.domain.model.MonthlyCashConsolidation;

import java.math.BigDecimal;

public record MonthlyConsolidationResponse(String period, BigDecimal ingresos, BigDecimal pagosOperacionales,
                                            BigDecimal gastos, BigDecimal devoluciones, BigDecimal total,
                                            long cancelaciones, BigDecimal costosOperacionales) {

    public static MonthlyConsolidationResponse from(MonthlyCashConsolidation consolidation) {
        return new MonthlyConsolidationResponse(consolidation.period(), consolidation.ingresos(),
                consolidation.pagosOperacionales(), consolidation.gastos(), consolidation.devoluciones(),
                consolidation.total(), consolidation.cancelaciones(), consolidation.costosOperacionales());
    }
}
