package com.corhuila.errorcapa8.travesia_natural.common.audit;

import java.util.List;

/**
 * Puerto de salida de auditoría. Vive en {@code common} porque cualquier módulo del
 * monolito puede necesitar dejar evidencia auditable, no solo {@code tenants} (plan
 * técnico de spec 002).
 */
public interface AuditRecorder {

    AuditRecord record(AuditRecord auditRecord);

    List<AuditRecord> findAll();
}
