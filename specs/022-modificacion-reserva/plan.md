# 022 — Plan técnico

## Enfoque

Mismo patrón que spec 011 (cancelación): una única transición del propio agregado
`Reservation`, con 3 campos nuevos y nullable en la tabla `reservations`
(`modification_reason`, `modified_by`, `modified_at`) y un método
`Reservation.modify(reservedServices, projectedValue, finalValue, reason, actorId)` que
valida estado/transferencia pendiente y reutiliza la misma fórmula de saldo a favor que
`cancel()`. La diferencia real frente a spec 011 es que esta operación SÍ reemplaza
`reservedServices`, algo que `ReservationRepositoryAdapter` documenta hoy como
deliberadamente no soportado ("ningún método de dominio lo modifica después") para
evitar que `orphanRemoval` borre/reinserte los servicios en cada pago o cancelación. Se
resuelve detectando en el adapter si la lista de `ReservedService` del agregado cambió
(comparación estructural, los `record` de Java ya traen `equals()`) y, solo en ese caso,
reemplazando la colección de la entidad — sin tocar la firma de
`ReservationRepositoryPort.save(Reservation)` que ya usan cancelar/pagar/devolver/
finalizar.

## Cambios por repositorio

**Backend**, paquete `reservations`:

- `src/main/resources/db/migration/V19__add_reservation_modification_fields.sql`
- `domain/model/Reservation.java`: 3 campos nuevos (`modificationReason`,
  `modifiedBy`, `modifiedAt`), constructor/`create`/`reconstitute` actualizados, nuevo
  método `modify(List<ReservedService> reservedServices, BigDecimal projectedValue,
  BigDecimal finalValue, String reason, String actorId)`.
- `domain/exception/ReservationNotModifiableException.java` (nueva).
- `infrastructure/out/persistence/ReservationEntity.java`: 3 columnas + getters nuevos,
  `updateState(...)` extendido con esos 3 campos, nuevo método
  `replaceReservedServices(List<ReservedServiceEntity>)` (limpia la colección actual y
  agrega la nueva; `orphanRemoval = true` ya declarado se encarga de borrar las filas
  viejas).
- `infrastructure/out/persistence/ReservationRepositoryAdapter.java`: `applyChanges`
  pasa los 3 campos nuevos a `updateState`, y compara
  `reservation.reservedServices()` contra los de la entidad persistida; si difieren,
  llama a `replaceReservedServices(...)`. `toDomain` lee los 3 campos nuevos.
- `domain/port/in/ModifyReservationCommand.java` + `ModifyReservationUseCase.java`
  (nuevos).
- `application/ModifyReservationService.java` (nuevo, mismo patrón que
  `CancelReservationService`).
- `infrastructure/in/web/ReservationController.java`: nuevo endpoint
  `POST /{reservationId}/modify` (reutiliza el `toDomainReservedServices(...)` privado
  ya existente) + manejo de `ReservationNotModifiableException`.
- `infrastructure/in/web/dto/ModifyReservationRequest.java` (nuevo) y
  `ReservationResponse.java` (extendido con `modificationReason`/`modifiedBy`/
  `modifiedAt`).
- `PLAN-VERIFICACION.md`: nueva sección "022 — Modificación de reserva antes de
  ejecución".

## Decisiones técnicas

1. **Detectar el cambio de `reservedServices` por comparación estructural en el
   adapter, no agregando un parámetro al puerto.** Alternativa descartada: extender
   `ReservationRepositoryPort.save(...)` con un flag `replaceReservedServices` o crear
   un método `saveWithServices(...)` aparte. Motivo: todos los demás casos de uso
   (pagar, cancelar, devolver, finalizar) ya llaman a `save(reservation)` sin saber
   nada de esto; forzarlos a pasar un flag nuevo ensucia una firma que hoy es estable.
   Comparar `List<ReservedService>` funciona directo porque es un `record` (spec 001) y
   ya trae `equals()` estructural.
2. **`modify()` reutiliza exactamente la fórmula de saldo a favor de `cancel()`**
   (`amountAlreadyPaid = finalValue - pendingBalance`, con los valores ANTERIORES).
   Alternativa descartada: una fórmula propia para modificación. Motivo: es
   matemáticamente el mismo problema ("¿cuánto dinero recibido queda por encima del
   nuevo valor final?"), spec 023 lo reutiliza también — mismo criterio en las tres
   specs nuevas.
3. **`projectedValue`/`finalValue` nuevos se aceptan como dato de entrada, no se
   recalculan con reglas de descuento.** Alternativa descartada: invocar algún motor
   de descuentos. Motivo: no existe ese motor (spec 008 lo dejó fuera de alcance) y
   `Reservation.create()` ya sigue el mismo criterio desde spec 001.
4. **Excepción propia `ReservationNotModifiableException`, no reutilizar
   `ReservationNotCancellableException`.** Alternativa descartada: un solo tipo para
   ambas operaciones. Motivo: son dos casos de uso distintos con sus propios mensajes de
   error (`reservation_not_cancellable` vs `reservation_not_modifiable`); mismo criterio
   de separación que ya existe entre `ReservationNotCancellableException`,
   `ReservationNotExecutableException` y `ReservationNotFinalizableException`.

## Modelo de datos

`V19__add_reservation_modification_fields.sql`:

```sql
ALTER TABLE reservations
    ADD COLUMN modification_reason VARCHAR(500),
    ADD COLUMN modified_by VARCHAR(255),
    ADD COLUMN modified_at TIMESTAMPTZ;
```

## Contratos

`POST /api/tenants/{tenantId}/reservations/{reservationId}/modify`

Request:
```json
{
  "reservedServices": [
    { "serviceReference": "cenotes", "partySize": 3, "scheduledDate": "2026-09-19" }
  ],
  "projectedValue": 1560000,
  "finalValue": 1560000,
  "reason": "Cliente cambió de tour por disponibilidad",
  "actorId": "operador-1"
}
```

Response `200 OK` — `ReservationResponse` con `reservedServices`/`projectedValue`/
`finalValue` actualizados, `modificationReason`/`modifiedBy`/`modifiedAt` informados,
`pendingBalance`/`creditBalance`/`paymentStatus`/`refundDecisionStatus` recalculados
según la fórmula de saldo a favor.

Errores:
- `400 validation_error` — falta `reason`, `reservedServices` vacío o `projectedValue`
  negativo.
- `404 not_found` — tenant o reserva inexistente.
- `409 tenant_inactive` — tenant `Inactivo`.
- `409 reservation_not_modifiable` — estado distinto de `Pendiente de pago`/
  `Confirmada`, o transferencia con soporte pendiente de aprobar/rechazar.

## Cómo se verifica

Un `curl` por cada criterio de aceptación de `spec.md`, agregado como nueva sección
"022" en `PLAN-VERIFICACION.md` (T05), ejecutado manualmente contra el servidor local
en T06 junto con `./mvnw test`.
