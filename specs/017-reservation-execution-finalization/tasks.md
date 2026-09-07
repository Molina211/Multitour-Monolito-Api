# 017 — Tareas

- [x] T01 — Migración `V14__add_reservation_finalization_fields.sql` +
      `Reservation.finalizeExecution(String actorId)` (valida estado `EN_EJECUCION`,
      fija `reservationStatus = FINALIZADA`, `finalizedBy`, `finalizedAt`) +
      `ReservationNotFinalizableException` · repo: backend · ~25 min
- [x] T02 — Actualizar `ReservationEntity` + `ReservationRepositoryAdapter` para
      persistir/leer `finalizedBy`/`finalizedAt` · repo: backend · depende de T01 ·
      ~15 min

  *(fin lote 1: T01-T02)*

- [x] T03 — Puerto in + servicio: `FinalizeReservationCommand`/
      `FinalizeReservationUseCase`/`FinalizeReservationService` (valida tenant activo,
      busca reserva, llama `finalize()`, guarda) · repo: backend · depende de T02 ·
      ~20 min
- [x] T04 — Endpoint `POST /{reservationId}/finalize` en `ReservationController` +
      `FinalizeReservationRequest` + extender `ReservationResponse` con
      `finalizedBy`/`finalizedAt` + manejo de `ReservationNotFinalizableException`
      (409) · repo: backend · depende de T03 · ~25 min

  *(fin lote 2: T03-T04)*

- [x] T05 — Extender `ExecutionResponse` con `finalized`/`finalizedBy`/`finalizedAt`
      (derivados de la `Reservation` asociada) + actualizar
      `OperationController.getExecution` para cargar la `Reservation` vía
      `reservationQueryUseCase` y pasarla a `ExecutionResponse.from(execution,
      reservation)` · repo: backend · depende de T04 · ~20 min
- [x] T06 — Actualizar `PLAN-VERIFICACION.md` con la sección "017 — Finalización de la
      ejecución de una reserva": un `curl` por cada criterio de aceptación · repo:
      backend · depende de T05 · ~20 min

  *(fin lote 3: T05-T06)*

- [x] T07 — Verificación final: `./mvnw test` en verde y ejecución manual de
      `PLAN-VERIFICACION.md` sección 017 contra el servidor local; marcar los criterios
      de aceptación de `spec.md` como cumplidos · repo: backend · depende de T06 ·
      requiere permiso explícito para build/tests/servidor (regla 5 de CLAUDE.md) ·
      ~25 min
