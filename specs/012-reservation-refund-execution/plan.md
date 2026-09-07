# 012 — Plan técnico

## Enfoque

Mismo tipo de cambio que spec 011: una única transición del propio agregado
`Reservation`, no un módulo nuevo. La ejecución de devolución es, igual que la
cancelación, un evento terminal por saldo a favor (una vez ejecutada, `paymentStatus`
deja de ser `SALDO_A_FAVOR_PENDIENTE`, así que no se puede volver a invocar sobre la
misma reserva). Se agregan cinco columnas nuevas y nullable a `reservations`
(`refunded_amount`, `refund_reason`, `refunded_by`, `refund_method`, `refunded_at`), un
método `Reservation.refund(amount, reason, actorId, method)` que valida precondición y
monto antes de mutar, y un único endpoint nuevo agregado al `ReservationController`
existente.

## Cambios por repositorio

**Backend** (`hu-back-001-dev`), paquete `reservations`:

- `src/main/resources/db/migration/V11__add_reservation_refund_fields.sql`
- `domain/model/Reservation.java`: 5 campos nuevos, constructor/`create`/`reconstitute`
  actualizados, nuevo método `refund(BigDecimal amount, String reason, String actorId, String method)`.
- `domain/exception/ReservationNotRefundableException.java` (nueva).
- `infrastructure/out/persistence/ReservationEntity.java` +
  `ReservationRepositoryAdapter.java`: persistir/leer los 5 campos nuevos.
- `domain/port/in/RefundReservationCommand.java` + `RefundReservationUseCase.java`
  (nuevos).
- `application/RefundReservationService.java` (nuevo).
- `infrastructure/in/web/ReservationController.java`: nuevo endpoint
  `POST /{reservationId}/refund` + manejo de `ReservationNotRefundableException`.
- `infrastructure/in/web/dto/RefundReservationRequest.java` (nuevo) y
  `ReservationResponse.java` (extendido con los 5 campos nuevos).
- `PLAN-VERIFICACION.md`: nueva sección "012 — Ejecución de devolución sobre saldo a
  favor".

## Decisiones técnicas

1. **Campos directos en `Reservation`, no módulo aparte.** Alternativa descartada:
   módulo `refunds`/`caja` estilo `operations`. Motivo: mismo criterio que spec 011 —
   es un evento único y terminal por saldo a favor, no un historial acumulable; y no
   existe ningún módulo de Caja real contra el cual registrar un movimiento (spec 012
   lo deja fuera de alcance explícitamente).
2. **Excepción propia `ReservationNotRefundableException`, no reutilizar
   `ReservationNotCancellableException`.** Alternativa descartada: un solo tipo de
   excepción de "estado inválido" para cancelar y devolver. Motivo: son dos casos de
   uso distintos con causas de negocio distintas (igual que
   `ReservationNotExecutableException` es un tipo aparte de
   `ReservationNotCancellableException` en spec 010/011); un mensaje describe el
   estado real (`paymentStatus` actual) y por qué no admite devolución.
3. **Un solo campo de monto (`refundedAmount`), no un historial de devoluciones.**
   Alternativa descartada: tabla `refund_executions` con una fila por ejecución.
   Motivo: la precondición ya obliga a que solo pueda ejecutarse una devolución por
   saldo a favor generado (tras ejecutarse, `paymentStatus` ya no es
   `SALDO_A_FAVOR_PENDIENTE`), así que nunca hay más de un evento de devolución vivo
   por reserva; un historial sería especular sobre un caso que la propia spec no
   permite.
4. **Validación de monto en el dominio (`Reservation.refund`), no en el DTO.**
   Alternativa descartada: `@DecimalMin`/`@Max` en `RefundReservationRequest`. Motivo:
   el tope depende de `creditBalance`, un valor de negocio, no una constante; mismo
   criterio que `registerCashPayment`/`registerInstallmentPayment`, que validan monto
   dentro del agregado.
5. **No se usa `AuditRecorder`.** Igual que spec 011: motivo, actor, monto, método y
   fecha quedan persistidos directamente en la reserva, que es donde los criterios de
   aceptación exigen poder consultarlos.

## Modelo de datos

`V11__add_reservation_refund_fields.sql`:

```sql
ALTER TABLE reservations
    ADD COLUMN refunded_amount NUMERIC(12,2),
    ADD COLUMN refund_reason VARCHAR(500),
    ADD COLUMN refunded_by VARCHAR(255),
    ADD COLUMN refund_method VARCHAR(100),
    ADD COLUMN refunded_at TIMESTAMP;
```

## Contratos

`POST /api/tenants/{tenantId}/reservations/{reservationId}/refund`

Request:
```json
{ "amount": 150000, "reason": "Devolución acordada con el cliente", "actorId": "admin-1", "method": "Transferencia" }
```

Response `200 OK` — `ReservationResponse` extendido con `refundedAmount`,
`refundReason`, `refundedBy`, `refundMethod`, `refundedAt`; `creditBalance` reducido por
`amount`; `paymentStatus: "Devuelto parcial o total"`.

Errores:
- `400 validation_error` — falta `reason`/`actorId`/`amount`, `amount` no positivo, o
  `amount` mayor al `creditBalance` disponible.
- `404 not_found` — tenant o reserva inexistente.
- `409 tenant_inactive` — tenant `Inactivo`.
- `409 reservation_not_refundable` — `paymentStatus` distinto de `Saldo a favor
  pendiente` (incluye `creditBalance == 0` o una reserva ya `Devuelto parcial o
  total`).

## Cómo se verifica

Un `curl` por cada criterio de aceptación de `spec.md`, agregado como nueva sección
"012" en `PLAN-VERIFICACION.md` (T05), ejecutado manualmente contra el servidor local en
T06 junto con `./mvnw test`.
