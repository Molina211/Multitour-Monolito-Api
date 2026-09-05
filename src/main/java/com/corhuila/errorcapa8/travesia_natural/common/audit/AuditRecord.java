package com.corhuila.errorcapa8.travesia_natural.common.audit;

import java.time.Instant;
import java.util.UUID;

/**
 * Entrada de auditoría (06-data/models.md, "Audit and traceability"). Compartida entre
 * módulos: cualquier acción que un módulo del monolito considere auditable pasa por
 * aquí, no solo las de {@code tenants} (spec 002).
 */
public record AuditRecord(UUID auditRecordId, String tenantId, String actorId, String action,
                           String affectedRecordId, String reason, Instant recordedAt,
                           String previousValue, String newValue, String channelOrModule,
                           String functionalProcessReference) {

    public static AuditRecord of(String tenantId, String actorId, String action, String affectedRecordId,
                                  String reason) {
        return of(tenantId, actorId, action, affectedRecordId, reason, null, null, null, null);
    }

    public static AuditRecord of(String tenantId, String actorId, String action, String affectedRecordId,
                                  String reason, String previousValue, String newValue, String channelOrModule,
                                  String functionalProcessReference) {
        return new AuditRecord(UUID.randomUUID(), tenantId, actorId, action, affectedRecordId, reason,
                Instant.now(), previousValue, newValue, channelOrModule, functionalProcessReference);
    }
}
