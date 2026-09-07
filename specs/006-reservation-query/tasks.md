# 006 — Tareas

**Batching (excepción regla 4 de `CLAUDE.md` — Backend, implementación inicial):**
lotes de 2-3 tareas, commit + push sin pedir permiso en cada uno, siempre que
`PLAN-VERIFICACION.md` esté al día en el repo al momento del push.

- [x] T01 — Migración `V5__alter_reservations_tenant_id.sql` (recrea `reservations`/
  `reserved_services` con `tenant_id VARCHAR(50) REFERENCES tenants`) · repo: backend
  · ~15 min
- [x] T02 — `Reservation.tenantId()` de `UUID` a `String`, agrega
  `Reservation.reconstitute(...)`; `CreateReservationCommand.tenantId` de `UUID` a
  `String` · repo: backend · ~15 min · depende de T01
- [x] T03 — `ReservationNotFoundException` (dominio) · repo: backend · ~5 min
- [x] T04 — `ReservationEntity`/`ReservedServiceEntity`: `tenantId` de `UUID` a
  `String`; `ReservationRepositoryPort` gana `findByTenantIdAndReservationId` y
  `findAllByTenantId`; `ReservationJpaRepository` con las derived queries
  correspondientes · repo: backend · ~20 min · depende de T02, T03
- [x] T05 — `ReservationRepositoryAdapter` implementa los dos métodos nuevos del
  puerto (mapeo hacia/desde el dominio vía `Reservation.reconstitute`) · repo: backend
  · ~15 min · depende de T04
- [x] T06 — `CreateReservationService` resuelve tenant vía `TenantRepositoryPort`
  (rechaza inexistente/`Inactivo`, igual que `CreateCatalogItemService`) · repo:
  backend · ~15 min · depende de T05
- [x] T07 — `ReservationQueryUseCase` (puerto) y `ReservationQueryService`
  (`getById`, `listByTenant`, mismo patrón que `CatalogItemQueryService`) · repo:
  backend · ~20 min · depende de T06
- [x] T08 — `ReservationController`: mueve `tenantId` del header a la URL
  (`/api/tenants/{tenantId}/reservations`), agrega `GET`/`GET /{reservationId}`,
  `ReservedServiceResponse`, `ReservationResponse.tenantId` a `String`, manejadores
  `404`/`409` · repo: backend · ~25 min · depende de T07

  **Nota de ejecución (desvío del batching original):** T01-T08 se implementaron y
  commitearon juntos en un solo lote, no en 4 lotes de 2-3 como preveía el plan. Motivo:
  cambiar el tipo de `tenantId` (`UUID` → `String`) en `Reservation` es un cambio que
  atraviesa dominio, persistencia, aplicación y web a la vez — cualquier corte
  intermedio (p. ej. commitear solo T02) deja el proyecto sin compilar, lo cual viola la
  regla dura "cada tarea deja el repo en estado compilable" (CLAUDE.md, sección 4) con
  más fuerza que la conveniencia de lotes pequeños. Se prefirió un commit que sí
  compila y pasa `./mvnw test` sobre cuatro que no.
  **— T01-T08: commit + push —**
- [x] T09 — Agrega la sección "006 — Consulta de reservas" a
  `PLAN-VERIFICACION.md` con los `curl` de cada criterio de aceptación · repo:
  backend · ~20 min · depende de T08
- [x] T10 — Verifica que `./mvnw test` sigue en verde (specs 001-006) y ejecuta la
  sección nueva de `PLAN-VERIFICACION.md` de punta a punta contra el servidor local
  · repo: backend · ~25 min · depende de T09
  **— fin lote (T09-T10): commit + push —**
