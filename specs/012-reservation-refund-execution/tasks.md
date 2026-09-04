# 012 — Tareas

- [x] T01 — Migración `V11__add_reservation_refund_fields.sql` +
      `Reservation.refund(BigDecimal amount, String reason, String actorId, String method)`
      (valida `paymentStatus == SALDO_A_FAVOR_PENDIENTE`, `creditBalance > 0`, `amount`
      positivo y `<= creditBalance`) + `ReservationNotRefundableException` · repo:
      backend · ~30 min
- [x] T02 — Actualizar `ReservationEntity` + `ReservationRepositoryAdapter` para
      persistir/leer `refundedAmount`/`refundReason`/`refundedBy`/`refundMethod`/
      `refundedAt` · repo: backend · depende de T01 · ~20 min

  *(fin lote 1: T01-T02)*

- [x] T03 — Puerto in + servicio: `RefundReservationCommand`/`RefundReservationUseCase`/
      `RefundReservationService` (valida tenant activo, busca reserva, llama
      `refund()`, guarda) · repo: backend · depende de T02 · ~25 min
- [x] T04 — Endpoint `POST /{reservationId}/refund` en `ReservationController` +
      `RefundReservationRequest` + extender `ReservationResponse` con los 5 campos
      nuevos + manejo de `ReservationNotRefundableException` (409) · repo: backend ·
      depende de T03 · ~25 min

  *(fin lote 2: T03-T04)*

- [x] T05 — Actualizar `PLAN-VERIFICACION.md` con la sección "012 — Ejecución de
      devolución sobre saldo a favor": un `curl` por cada criterio de aceptación ·
      repo: backend · depende de T04 · ~20 min

- [ ] T06 — Verificación final: `./mvnw test` en verde y ejecución manual de
      `PLAN-VERIFICACION.md` sección 012 contra el servidor local; marcar los
      criterios de aceptación de `spec.md` como cumplidos · repo: backend · depende de
      T05 · requiere permiso explícito para build/tests/servidor (regla 5 de
      CLAUDE.md) · ~25 min
