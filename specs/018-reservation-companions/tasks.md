# 018 — Tareas

- [x] T01 — Migración `V15__add_reservation_companions.sql` + record `Companion` +
      extender `Reservation` (constructor, `create()`, `reconstitute()`, getters) con
      `holderDocument`/`companions`, incluyendo la validación de no-duplicidad de
      documento (RN-RES-005) en `create()` vía `InvalidReservationException` · repo:
      backend · ~25 min
- [x] T02 — `CompanionEntity` (mismo patrón que `ReservedServiceEntity`) + extender
      `ReservationEntity` (columna `holder_document`, colección `companions`,
      `addCompanion()`) + actualizar `ReservationRepositoryAdapter`
      (`toNewEntity`/`toDomain`) para persistir/leer ambos campos · repo: backend ·
      depende de T01 · ~20 min

  *(fin lote 1: T01-T02)*

- [x] T03 — Extender `CreateReservationCommand` y `CreateReservationService` con
      `holderDocument`/`companions` · repo: backend · depende de T02 · ~10 min
- [x] T04 — `CompanionRequest`/`CompanionResponse` (nuevos) + extender
      `CreateReservationRequest` y `ReservationResponse` + actualizar
      `ReservationController.create()` para mapear y pasar los nuevos campos · repo:
      backend · depende de T03 · ~20 min

  *(fin lote 2: T03-T04)*

- [x] T05 — Actualizar `PLAN-VERIFICACION.md` con la sección "018 — Acompañantes
      individualizados en la reserva": un `curl` por cada criterio de aceptación · repo:
      backend · depende de T04 · ~20 min
- [x] T06 — Verificación final: `./mvnw test` en verde y ejecución manual de
      `PLAN-VERIFICACION.md` sección 018 contra el servidor local; marcar los criterios
      de aceptación de `spec.md` como cumplidos · repo: backend · depende de T05 ·
      requiere permiso explícito para build/tests/servidor (regla 5 de CLAUDE.md) ·
      ~25 min

  *(fin lote 3: T05-T06)*
