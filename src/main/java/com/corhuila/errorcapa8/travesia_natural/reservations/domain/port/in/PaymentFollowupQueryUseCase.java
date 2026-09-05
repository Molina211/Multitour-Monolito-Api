package com.corhuila.errorcapa8.travesia_natural.reservations.domain.port.in;

import com.corhuila.errorcapa8.travesia_natural.common.audit.AuditRecord;

import java.util.List;
import java.util.UUID;

public interface PaymentFollowupQueryUseCase {

    List<AuditRecord> listFollowups(String tenantId, UUID reservationId);
}
