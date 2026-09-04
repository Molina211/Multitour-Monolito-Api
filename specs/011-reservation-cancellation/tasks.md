# 011 — Tareas

- [x] T01 — Migración `V10__add_reservation_cancellation_fields.sql` +
      `Reservation.cancel(String reason, String actorId)` (valida estado
      `PENDIENTE_DE_PAGO`/`CONFIRMADA` y `pendingTransferAmount == null`, calcula
      `creditBalance`/`paymentStatus`) + `ReservationNotCancellableException` · repo:
      backend · ~30 min
- [x] T02 — Actualizar `ReservationEntity` + `ReservationRepositoryAdapter` para
      persistir/leer `cancellationReason`/`cancelledBy`/`cancelledAt` · repo: backend ·
      depende de T01 · ~20 min

  *(fin lote 1: T01-T02)*

- [x] T03 — Puerto in + servicio: `CancelReservationCommand`/`CancelReservationUseCase`/
      `CancelReservationService` (valida tenant activo, busca reserva, llama
      `cancel()`, guarda) · repo: backend · depende de T02 · ~25 min
- [x] T04 — Endpoint `POST /{reservationId}/cancel` en `ReservationController` +
      `CancelReservationRequest` + extender `ReservationResponse` con los 3 campos
      nuevos + manejo de `ReservationNotCancellableException` (409) · repo: backend ·
      depende de T03 · ~25 min

  *(fin lote 2: T03-T04)*

- [x] T05 — Actualizar `PLAN-VERIFICACION.md` con la sección "011 — Cancelación de
      reserva antes de ejecución": un `curl` por cada criterio de aceptación · repo:
      backend · depende de T04 · ~20 min

- [x] T06 — Verificación final: `./mvnw test` en verde y ejecución manual de
      `PLAN-VERIFICACION.md` sección 011 contra el servidor local; marcar los
      criterios de aceptación de `spec.md` como cumplidos · repo: backend · depende de
      T05 · requiere permiso explícito para build/tests/servidor (regla 5 de
      CLAUDE.md) · ~25 min
