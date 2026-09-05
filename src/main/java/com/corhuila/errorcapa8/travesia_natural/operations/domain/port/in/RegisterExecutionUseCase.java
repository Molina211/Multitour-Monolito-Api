package com.corhuila.errorcapa8.travesia_natural.operations.domain.port.in;

import com.corhuila.errorcapa8.travesia_natural.operations.domain.model.Execution;

public interface RegisterExecutionUseCase {

    Execution registerExecution(RegisterExecutionCommand command);
}
