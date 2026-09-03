package com.corhuila.errorcapa8.travesia_natural.reservations.infrastructure.in.web.dto;

import com.corhuila.errorcapa8.travesia_natural.common.audit.AuditRecord;

import java.time.Instant;

public record PaymentFollowupResponse(String note, String actorId, Instant recordedAt) {

    public static PaymentFollowupResponse from(AuditRecord record) {
        return new PaymentFollowupResponse(record.reason(), record.actorId(), record.recordedAt());
    }
}
