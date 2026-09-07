# 024 — Tareas

- [x] T01 — Migración `V20__add_transport_fields_to_reserved_services.sql` + extender
      `ReservedService` (record) con `transportItemId`/`transportCost` (ambos nullable,
      sin nueva validación) · repo: backend · ~15 min
- [x] T02 — Extender `ReservedServiceEntity` (columnas/constructor/getters) + actualizar
      los 4 puntos de mapeo en `ReservationRepositoryAdapter` (`applyChanges`,
      `toNewEntity`, `toDomain`) para persistir/leer ambos campos · repo: backend ·
      depende de T01 · ~20 min

  *(fin lote 1: T01-T02)*

- [x] T03 — Extender `CreateReservationService` con `CatalogItemRepositoryPort`: por cada
      `ReservedService` con `transportItemId`, buscar el `CatalogItem` por
      `tenantId`+`catalogItemId`, validar `type == TRANSPORT` y `active == true`
      (cualquier fallo → `InvalidReservationException`), calcular `transportCost =
      price * partySize` · repo: backend · depende de T02 · ~20 min
- [x] T04 — `ReservedServiceRequest` (agregar `transportItemId`) + `ReservedServiceResponse`
      (agregar `transportItemId`/`transportCost`, actualizar `from()`) + confirmar que
      `ReservationController.create()` sigue mapeando sin cambios adicionales · repo:
      backend · depende de T03 · ~15 min

  *(fin lote 2: T03-T04)*

- [x] T05 — Actualizar `PLAN-VERIFICACION.md` con la sección "024 — Transporte en
      reserva": un `curl` por cada criterio de aceptación (con transporte válido, sin
      transporte, y los 4 sub-casos de `transportItemId` inválido) · repo: backend ·
      depende de T04 · ~20 min
- [x] T06 — Verificación final: `./mvnw test` en verde y ejecución manual de
      `PLAN-VERIFICACION.md` sección 024 contra el servidor local; marcar los criterios
      de aceptación de `spec.md` como cumplidos · repo: backend · depende de T05 ·
      requiere permiso explícito para build/tests/servidor (regla 5 de CLAUDE.md) ·
      ~20 min

  *(fin lote 3: T05-T06)*
