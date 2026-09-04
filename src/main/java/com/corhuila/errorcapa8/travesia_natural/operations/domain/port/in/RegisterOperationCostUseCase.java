package com.corhuila.errorcapa8.travesia_natural.operations.domain.port.in;

import com.corhuila.errorcapa8.travesia_natural.operations.domain.model.OperationCost;

public interface RegisterOperationCostUseCase {

    OperationCost registerCost(RegisterOperationCostCommand command);
}
