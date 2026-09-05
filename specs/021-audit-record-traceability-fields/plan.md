# 021 — Plan técnico

## Enfoque

Se amplía el `record` `AuditRecord` con los cuatro campos que `06-data/models.md` ya
confirma (`previousValue`, `newValue`, `channelOrModule`, `functionalProcessReference`),
todos opcionales, y se propagan a la entidad JPA, el adaptador y la respuesta de
`GET /api/audit` (que ya devuelve el `record` completo, sin DTO intermedio). Se mantiene el
método `AuditRecord.of(...)` actual de 5 argumentos (delegando a uno nuevo con los campos
extra en `null`) para no romper los ~10 call-sites existentes de una sola vez, y se
actualiza un único caso de uso de ejemplo (`DeactivateTenantService`/
`ReactivateTenantService`) para demostrar el uso real de `previousValue`/`newValue`.

## Cambios por repositorio

Solo backend (`common/audit` + un caso de uso de ejemplo en `tenants`).

- `common/audit/AuditRecord.java`: agrega `previousValue`, `newValue`, `channelOrModule`,
  `functionalProcessReference` (todos `String`, nullable); nuevo `of(...)` con 9
  argumentos; el `of(...)` de 5 argumentos existente delega al nuevo pasando `null` en los
  cuatro campos nuevos.
- `common/audit/infrastructure/AuditRecordEntity.java` +
  `common/audit/infrastructure/AuditRecorderAdapter.java`: columnas nuevas.
- `src/main/resources/db/migration/V18__add_audit_record_traceability_fields.sql`.
- `tenants/application/DeactivateTenantService.java` y
  `tenants/application/ReactivateTenantService.java`: pasan `previousValue`/`newValue`
  reales (`"Activo"`/`"Inactivo"`), `channelOrModule = "tenants"`,
  `functionalProcessReference = "Cambio de estado de operador"` (mismo texto que ya usa
  `platform-data.service.ts` en el Frontend, para no inventar otro).

`AuditController` no necesita cambios: expone el `record` completo tal cual, así que los
campos nuevos aparecen automáticamente en `GET /api/audit`.

## Decisiones técnicas

- **Overload de `of(...)`** en vez de cambiar la firma existente de una sola vez —
  descartado actualizar los ~10 call-sites en esta misma spec porque queda fuera de
  alcance (ver spec); se actualizan de forma incremental cuando se toque cada módulo.
- **Nombres de campo iguales a Docs** (`channelOrModule`, `functionalProcessReference`) en
  vez de los nombres que usa el Frontend (`module`, `functionalReference`) — Docs es la
  fuente de verdad (regla 7/CLAUDE.md); no se renombra para igualar al Frontend.
- **No se agrega `actorRole`** — ya acordado con el usuario, no está en el esquema
  confirmado de Docs.

## Modelo de datos

`V18__add_audit_record_traceability_fields.sql`:

```sql
ALTER TABLE audit_records
    ADD COLUMN previous_value VARCHAR(500),
    ADD COLUMN new_value VARCHAR(500),
    ADD COLUMN channel_or_module VARCHAR(150),
    ADD COLUMN functional_process_reference VARCHAR(255);
```

## Contratos

- `GET /api/audit` → cada elemento de la lista incluye ahora `previousValue`, `newValue`,
  `channelOrModule`, `functionalProcessReference` (`null` si no se informaron). Sin cambio
  de status codes ni de los campos existentes.

## Cómo se verifica

Se agrega una sección "021" a `PLAN-VERIFICACION.md`:
1. Consultar `GET /api/audit` antes del cambio y confirmar que los registros ya existentes
   se siguen leyendo sin error (campos nuevos en `null`).
2. Desactivar un tenant activo y volver a consultar `GET /api/audit`: el registro nuevo
   debe traer `previousValue: "Activo"`, `newValue: "Inactivo"`,
   `channelOrModule: "tenants"`, `functionalProcessReference: "Cambio de estado de
   operador"`.
3. Reactivarlo y repetir, confirmando `previousValue: "Inactivo"`, `newValue: "Activo"`.
