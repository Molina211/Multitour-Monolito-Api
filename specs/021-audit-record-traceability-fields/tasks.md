# 021 — Tareas

- [ ] T01 — Ampliar `AuditRecord` con los 4 campos nuevos + overload de `of(...)`
      (manteniendo el de 5 argumentos por compatibilidad) · repo: backend · ~20 min
- [ ] T02 — `AuditRecordEntity` + `AuditRecorderAdapter`: mapear las 4 columnas nuevas ·
      repo: backend · ~25 min · depende de T01
- [ ] T03 — Migración `V18__add_audit_record_traceability_fields.sql` · repo: backend ·
      ~10 min · depende de T02
- [ ] T04 — Actualizar `DeactivateTenantService` y `ReactivateTenantService` para pasar
      `previousValue`/`newValue`/`channelOrModule`/`functionalProcessReference` reales ·
      repo: backend · ~20 min · depende de T01
- [ ] T05 — Verificar los 5 criterios de aceptación de la spec (`GET /api/audit` antes y
      después de activar/desactivar un tenant) y agregar la sección "021" a
      `PLAN-VERIFICACION.md` · repo: backend · ~15 min · depende de T03, T04
