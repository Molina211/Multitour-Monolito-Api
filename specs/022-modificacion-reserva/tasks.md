# 022 — Tareas

- [x] T01 — Migración `V19__add_reservation_modification_fields.sql` +
      `Reservation.modify(List<ReservedService>, BigDecimal projectedValue, BigDecimal
      finalValue, String reason, String actorId)` (valida estado `PENDIENTE_DE_PAGO`/
      `CONFIRMADA`, `pendingTransferAmount == null`, `reservedServices` no vacío,
      `projectedValue` no negativo; recalcula `pendingBalance`/`creditBalance`/
      `paymentStatus`/`refundDecisionStatus` con la fórmula de spec 011) +
      `ReservationNotModifiableException` · repo: backend · ~30 min
- [x] T02 — `ReservationEntity`: columnas + getters de `modificationReason`/
      `modifiedBy`/`modifiedAt`, `updateState(...)` extendido, nuevo método
      `replaceReservedServices(List<ReservedServiceEntity>)` · repo: backend · depende
      de T01 · ~20 min

  *(fin lote 1: T01-T02)*

- [x] T03 — `ReservationRepositoryAdapter`: `applyChanges` pasa los 3 campos nuevos,
      detecta cambio en `reservedServices` (comparación estructural) y llama
      `replaceReservedServices(...)`; `toDomain` lee los 3 campos nuevos · repo:
      backend · depende de T02 · ~25 min
- [x] T04 — Puerto in + servicio: `ModifyReservationCommand`/`ModifyReservationUseCase`/
      `ModifyReservationService` (valida tenant activo, busca reserva, llama
      `modify()`, guarda) · repo: backend · depende de T03 · ~20 min

  *(fin lote 2: T03-T04)*

- [x] T05 — Endpoint `POST /{reservationId}/modify` en `ReservationController` +
      `ModifyReservationRequest` (reutiliza `toDomainReservedServices` privado
      existente) + extender `ReservationResponse` con los 3 campos nuevos + manejo de
      `ReservationNotModifiableException` (409) · repo: backend · depende de T04 ·
      ~25 min
- [x] T06 — Actualizar `PLAN-VERIFICACION.md` con la sección "022 — Modificación de
      reserva antes de ejecución": un `curl` por cada criterio de aceptación · repo:
      backend · depende de T05 · ~20 min

  *(fin lote 3: T05-T06)*

- [x] T07 — Verificación final: `./mvnw test` en verde y ejecución manual de
      `PLAN-VERIFICACION.md` sección 022 contra el servidor local; marcar los
      criterios de aceptación de `spec.md` como cumplidos · repo: backend · depende de
      T06 · requiere permiso explícito para build/tests/servidor (regla 5 de
      CLAUDE.md) · ~25 min
