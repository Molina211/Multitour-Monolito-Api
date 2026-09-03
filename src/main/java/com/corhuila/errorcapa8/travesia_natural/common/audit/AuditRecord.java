package com.corhuila.errorcapa8.travesia_natural.common.audit;

import java.time.Instant;
import java.util.UUID;

/**
 * Entrada de auditoría (06-data/models.md, "Audit and traceability"). Compartida entre
 * módulos: cualquier acción que un módulo del monolito considere auditable pasa por
 * aquí, no solo las de {@code tenants} (spec 002).
 */
public record AuditRecord(UUID auditRecordId, String tenantId, String actorId, String action,
                           String affectedRecordId, String reason, Instant recordedAt) {

    public static AuditRecord of(String tenantId, String actorId, String action, String affectedRecordId,
                                  String reason) {
        return new AuditRecord(UUID.randomUUID(), tenantId, actorId, action, affectedRecordId, reason,
                Instant.now());
    }
}
