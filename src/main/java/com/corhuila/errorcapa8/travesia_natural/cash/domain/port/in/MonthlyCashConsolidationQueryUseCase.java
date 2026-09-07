package com.corhuila.errorcapa8.travesia_natural.cash.domain.port.in;

import com.corhuila.errorcapa8.travesia_natural.cash.domain.model.MonthlyCashConsolidation;

import java.time.YearMonth;
import java.util.List;

public interface MonthlyCashConsolidationQueryUseCase {

    List<MonthlyCashConsolidation> getMonthlyConsolidation(String tenantId, YearMonth period);
}
