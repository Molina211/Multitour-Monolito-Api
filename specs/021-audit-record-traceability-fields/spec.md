# 021 — Completar campos de trazabilidad de `AuditRecord` (previousValue, newValue, canal, proceso funcional)

**Estado:** APROBADA
**Fecha:** 2026-09-05
**Repos afectados:** backend
**HU relacionada:** ninguna HU formal en el backlog; cierra una brecha entre el Backend y
`06-data/models.md` (Docs, sección "Audit and traceability", ya CONFIRMADA).

## Problema

`06-data/models.md` (fuente de verdad documental) confirma el esquema de `audit_records`
con los campos `previousValue`, `newValue`, `channelOrModule` y `functionalProcessReference`,
además de los ya implementados. `AuditRecord.java` en el Backend solo tiene
`auditRecordId`, `tenantId`, `actorId`, `action`, `affectedRecordId`, `reason` y
`recordedAt` — le faltan esos cuatro campos que Docs ya da por confirmados. Esto no es solo
una brecha frente al Frontend: es una brecha frente a la propia fuente de verdad. La pantalla
de auditoría de plataforma del Frontend (`platform-data.service.ts`) ya espera
estructuralmente algo similar (`previousValue`, `newValue`, `module`, `functionalReference`),
más un campo `actorRole` que Docs no contempla.

## Alcance

- Ampliar `AuditRecord` con `previousValue`, `newValue`, `channelOrModule` y
  `functionalProcessReference`, todos opcionales (Docs los describe como "cuando aplique").
- Actualizar `AuditRecorder` y la persistencia (`AuditRecordEntity`,
  `AuditRecorderAdapter`/repositorio JPA) para guardar y recuperar los nuevos campos.
- Actualizar los puntos donde hoy se construye `AuditRecord.of(...)` para pasar
  `previousValue`/`newValue` cuando el propio caso de uso ya conoce el valor anterior y el
  nuevo (ej. activar/desactivar tenant, aprobar/rechazar soporte de pago); cuando no
  aplique, quedan en `null`, no se inventa un valor.
- Exponer los cuatro campos nuevos en la respuesta de `GET /api/audit`.

## Fuera de alcance

- `actorRole`: no está en el esquema confirmado de `06-data/models.md`. No se agrega en
  esta spec. Si se necesita para la pantalla de auditoría de plataforma, primero hay que
  actualizar Docs (regla 2/3 de `CLAUDE.md`: autorización siempre antes de escribir ahí),
  no improvisarlo en el Backend de paso.
- Reescribir o migrar registros de auditoría ya guardados sin estos campos: quedan con los
  campos nuevos en `null`, no se genera ni infiere un valor histórico.
- Modificar `06-data/models.md`: esta spec solo implementa lo que Docs ya confirma, no lo
  cambia ni lo reinterpreta.
- Actualizar exhaustivamente **todos** los casos de uso que auditan algo para que llenen
  `previousValue`/`newValue`: se hace al menos en un caso de uso de ejemplo (ver criterios
  de aceptación); el resto se completa de forma incremental en specs futuras según se vayan
  tocando esos módulos.

## Criterios de aceptación

- [ ] `AuditRecord` acepta y persiste `previousValue`, `newValue`, `channelOrModule` y
      `functionalProcessReference` como campos opcionales.
- [ ] `GET /api/audit` devuelve los cuatro campos nuevos (`null` cuando no se informaron).
- [ ] Activar o desactivar un `Tenant` (`TenantController`) registra `previousValue` y
      `newValue` reales del estado (ej. `Activo` → `Inactivo`), como caso de uso de
      ejemplo de los campos nuevos.
- [ ] Los registros de auditoría creados antes de esta spec se siguen leyendo sin error,
      con los campos nuevos en `null`.
- [ ] El proyecto compila y las specs 001-020 siguen pasando.

## Impacto en multitenencia

Ninguno nuevo: `AuditRecord` ya filtra por `tenantId` desde spec 002; los campos agregados
no cambian ese criterio de aislamiento.

## Riesgos y decisiones abiertas

1. ¿Se agrega `actorRole` de una vez, ya que el Frontend lo espera, aunque no esté en Docs?
   Recomendación: no — regla 7/8 de `CLAUDE.md` (no inventar contenido que no está
   verificado en la fuente de verdad); se actualiza Docs primero si realmente se necesita.
2. Además del ejemplo de activar/desactivar tenant, ¿qué otros casos de uso existentes se
   actualizan en esta misma spec para llenar `previousValue`/`newValue` (ej. aprobar/
   rechazar soporte de pago, cambiar estado de una reserva), y cuáles se dejan para cuando
   se vuelva a tocar ese módulo? Se decide en `/plan-tareas`.
3. Nombre y tipo exacto de columna para los cuatro campos nuevos en la entidad JPA — se
   resuelve en `/plan-tareas`, no cambia ningún criterio de aceptación.

## Evidencia para materia

Cierra la brecha Backend-vs-Docs del bloque de auditoría (`06-data/models.md`, sección
"Audit and traceability"); alinea el Backend con la fuente de verdad documental antes de
conectar la pantalla de auditoría de plataforma. Evidencia de trazabilidad/auditoría para
el Weekly y la sustentación.
