# 009 — Tareas

- [x] T01 — Agregar a `Reservation`: campos `pendingTransferAmount`/
      `transferSupportReference`, métodos `registerCashPayment`,
      `registerInstallmentPayment`, `registerTransferPayment`,
      `approveTransferPayment`, `rejectTransferPayment`, actualizar `reconstitute(...)`,
      y `PaymentAlreadyResolvedException` · repo: backend · ~30 min
- [x] T02 — Migración `V7__add_reservation_payment_fields.sql` +
      `ReservationEntity`/`ReservationRepositoryAdapter` con los dos campos nuevos ·
      repo: backend · depende de T01 · ~25 min

  *(fin lote 1: T01-T02)*

- [x] T03 — `RegisterPaymentCommand`/`RegisterPaymentUseCase`/`RegisterPaymentService` +
      `DecidePaymentSupportCommand`/`DecidePaymentSupportUseCase`/
      `DecidePaymentSupportService` (con registro en `AuditRecorder`) · repo: backend ·
      depende de T02 · ~30 min
- [x] T04 — `PaymentController` (endpoints `POST .../payments`,
      `POST .../payments/decide-support`, `GET /reservations/pending-support`) + DTOs
      request + manejo de excepciones (400/404/409) · repo: backend · depende de T03 ·
      ~30 min

  *(fin lote 2: T03-T04)*

- [x] T05 — `RegisterPaymentFollowupCommand`/`UseCase`/`Service` (vía `AuditRecorder`) +
      método de consulta de seguimientos (filtra `findAll()` por tenant/reserva/acción) +
      endpoints `POST`/`GET .../payments/followups` + DTOs de seguimiento · repo: backend
      · depende de T04 · ~25 min
- [x] T06 — Actualizar `PLAN-VERIFICACION.md` con la sección "009 — Registro de pago
      sobre una reserva": un `curl` por cada criterio de aceptación · repo: backend ·
      depende de T05 · ~20 min

  *(fin lote 3: T05-T06)*

- [x] T07 — Verificación final: `./mvnw test` en verde y ejecución manual de
      `PLAN-VERIFICACION.md` sección 009 contra el servidor local; marcar los criterios
      de aceptación de `spec.md` como cumplidos · repo: backend · depende de T06 ·
      requiere permiso explícito para build/tests/servidor (regla 5 de CLAUDE.md) ·
      ~25 min
