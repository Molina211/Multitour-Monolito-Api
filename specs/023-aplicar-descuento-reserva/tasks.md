# 023 — Tareas

- [x] T01 — `ReservationNotDiscountableException` +
      `Reservation.applyDiscount(int percentage, String reason, String actorId)` (valida
      `percentage` 1-100, `reason` no vacío, estado `PENDIENTE_DE_PAGO`/`CONFIRMADA`;
      recalcula `finalValue`/`pendingBalance`/`creditBalance`/`paymentStatus`/
      `refundDecisionStatus` con la fórmula de spec 011/022) · repo: backend · ~25 min
- [x] T02 — Puerto in + servicio: `ApplyDiscountCommand`/`ApplyDiscountUseCase`/
      `ApplyDiscountReservationService` (valida tenant activo, busca reserva, llama
      `applyDiscount()`, guarda, registra `AuditRecord` vía `AuditRecorder` existente) ·
      repo: backend · depende de T01 · ~20 min
- [x] T03 — Endpoint `POST /{reservationId}/apply-discount` en `ReservationController` +
      `ApplyDiscountRequest` + manejo de `ReservationNotDiscountableException` (409) ·
      repo: backend · depende de T02 · ~20 min
- [x] T04 — Actualizar `PLAN-VERIFICACION.md` con la sección "023 — Aplicar descuento
      adicional a una reserva": un `curl` por cada criterio de aceptación · repo:
      backend · depende de T03 · ~15 min
- [x] T05 — Verificación final: `./mvnw test` en verde y ejecución manual de
      `PLAN-VERIFICACION.md` sección 023 contra el servidor local; marcar los criterios
      de aceptación de `spec.md` como cumplidos · repo: backend · depende de T04 ·
      requiere permiso explícito para build/tests/servidor (regla 5 de CLAUDE.md) ·
      ~20 min
