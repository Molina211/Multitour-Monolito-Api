# 011 — Plan técnico

## Enfoque

A diferencia de spec 010 (que creó un módulo `operations` nuevo porque ejecución y
costos son eventos/registros recurrentes, separados del agregado), la cancelación es una
única transición terminal del propio agregado `Reservation` — mismo tipo de cambio que
ya hizo spec 009 al agregar campos de pago directamente sobre `Reservation`
(`pendingTransferAmount`, `transferSupportReference`, migración V7). Se sigue ese mismo
patrón: tres columnas nuevas y nullable en la tabla `reservations`
(`cancellation_reason`, `cancelled_by`, `cancelled_at`), un método
`Reservation.cancel(reason, actorId)` que valida estado y transferencia pendiente antes
de mutar, y un único endpoint nuevo agregado al `ReservationController` existente (no se
crea un controller aparte, porque es una sola operación sobre el mismo agregado, no una
familia de endpoints como pagos o ejecución).

## Cambios por repositorio

**Backend** (`hu-back-001-dev`), paquete `reservations`:

- `src/main/resources/db/migration/V10__add_reservation_cancellation_fields.sql`
- `domain/model/Reservation.java`: 3 campos nuevos, constructor/`create`/`reconstitute`
  actualizados, nuevo método `cancel(String reason, String actorId)`.
- `domain/exception/ReservationNotCancellableException.java` (nueva).
- `infrastructure/out/persistence/ReservationEntity.java` +
  `ReservationRepositoryAdapter.java`: persistir/leer los 3 campos nuevos.
- `domain/port/in/CancelReservationCommand.java` + `CancelReservationUseCase.java`
  (nuevos).
- `application/CancelReservationService.java` (nuevo).
- `infrastructure/in/web/ReservationController.java`: nuevo endpoint
  `POST /{reservationId}/cancel` + manejo de `ReservationNotCancellableException`.
- `infrastructure/in/web/dto/CancelReservationRequest.java` (nuevo) y
  `ReservationResponse.java` (extendido con los 3 campos nuevos).
- `PLAN-VERIFICACION.md`: nueva sección "011 — Cancelación de reserva antes de
  ejecución".

## Decisiones técnicas

1. **Campos directos en `Reservation`, no módulo aparte.** Alternativa descartada:
   módulo `cancellations` estilo `operations`. Motivo: es un evento único y terminal por
   reserva (no un historial acumulable como `Execution`/`OperationCost`), igual que los
   campos de transferencia de spec 009.
2. **Una sola excepción `ReservationNotCancellableException` para "estado inválido" y
   "transferencia pendiente".** Alternativa descartada: dos excepciones separadas.
   Motivo: ambos casos son el mismo código HTTP (`409`) con causas distintas en el
   mensaje; mismo criterio que `ReservationNotExecutableException` en spec 010, que
   cubre varias causas de estado inválido con un solo tipo.
3. **Endpoint agregado a `ReservationController` existente, no un controller nuevo.**
   Alternativa descartada: `CancellationController` aparte (patrón de
   `PaymentController`/`OperationController`). Motivo: es una única operación sobre el
   agregado, no una familia de endpoints de un dominio propio; no amerita separación.
4. **No se usa `AuditRecorder`.** Alternativa descartada: registrar la cancelación en el
   audit log compartido (como hace `DecidePaymentSupportService`). Motivo: el motivo y
   actor ya quedan persistidos directamente en la reserva (criterio de aceptación los
   exige ahí, no en una consulta aparte de auditoría); mismo criterio que
   `RegisterExecutionService`/`RegisterPaymentService`, que tampoco usan audit.

## Modelo de datos

`V10__add_reservation_cancellation_fields.sql`:

```sql
ALTER TABLE reservations
    ADD COLUMN cancellation_reason VARCHAR(500),
    ADD COLUMN cancelled_by VARCHAR(255),
    ADD COLUMN cancelled_at TIMESTAMPTZ;
```

## Contratos

`POST /api/tenants/{tenantId}/reservations/{reservationId}/cancel`

Request:
```json
{ "reason": "Cliente desistió del viaje", "actorId": "operador-1" }
```

Response `200 OK` — `ReservationResponse` extendido con `cancellationReason`,
`cancelledBy`, `cancelledAt`; `reservationStatus: "Cancelada"`; `creditBalance` con el
monto ya pagado si lo había, `pendingBalance: 0`, `paymentStatus: "Saldo a favor
pendiente"` si `creditBalance > 0`, sin cambios si no había pagos.

Errores:
- `400 validation_error` — falta `reason`.
- `404 not_found` — tenant o reserva inexistente.
- `409 tenant_inactive` — tenant `Inactivo`.
- `409 reservation_not_cancellable` — estado distinto de `Pendiente de pago`/
  `Confirmada`, o transferencia con soporte pendiente de aprobar/rechazar.

## Cómo se verifica

Un `curl` por cada criterio de aceptación de `spec.md`, agregado como nueva sección
"011" en `PLAN-VERIFICACION.md` (T05), ejecutado manualmente contra el servidor local en
T06 junto con `./mvnw test`.
