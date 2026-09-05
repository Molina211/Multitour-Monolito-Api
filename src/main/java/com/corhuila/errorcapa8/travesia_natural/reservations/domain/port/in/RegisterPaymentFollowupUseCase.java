package com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.in;

import com.corhuila.errorcapa8.travesia_natural.common.audit.AuditRecord;

public interface RegisterPaymentFollowupUseCase {

    AuditRecord registerFollowup(RegisterPaymentFollowupCommand command);
}
