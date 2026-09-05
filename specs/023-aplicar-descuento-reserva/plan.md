# 023 — Plan técnico

## Enfoque

Nuevo método de dominio `Reservation.applyDiscount(int percentage, String reason,
String actorId)` que recalcula `finalValue` a partir del valor ACTUAL (`newFinalValue =
finalValue * (100 - percentage) / 100`), reutilizando exactamente la misma fórmula de
saldo a favor de `cancel()`/`modify()` (spec 011/022) para `pendingBalance`/
`creditBalance`/`paymentStatus`/`refundDecisionStatus`. Se permite aplicar el descuento
más de una vez sobre la misma reserva (decisión abierta 1 de la spec, confirmada aquí):
cada aplicación se calcula sobre el `finalValue` vigente en ese momento, sin bloquear
aplicaciones repetidas — el propio Frontend tampoco lo bloquea.

No se agrega ningún campo nuevo a `Reservation`/`ReservationEntity` ni migración: el
registro de trazabilidad de cada aplicación (decisión abierta 2) se resuelve escribiendo
una entrada en el módulo compartido `common/audit` (`AuditRecorder`/`AuditRecord`, ya
usado por `tenants` desde spec 002/021), con `action: "RESERVATION_DISCOUNT_APPLIED"`,
`affectedRecordId` = `reservationId`, `previousValue`/`newValue` = `finalValue` antes/
después, y el `percentage` embebido en `functionalProcessReference` (ej. "Aplicación de
descuento adicional: 10%"). Evita inventar una lista nueva tipo `companions` solo para
guardar historial que ya tiene un lugar genérico en el proyecto (regla 10 de CLAUDE.md:
preferir lo simple).

## Cambios por repositorio

**Backend** (único repo afectado, según la spec):

- `reservations/domain/exception/ReservationNotDiscountableException.java` (nuevo):
  mismo patrón que `ReservationNotModifiableException`/`ReservationNotCancellableException`.
- `reservations/domain/model/Reservation.java`: nuevo método `applyDiscount(...)`. No
  cambia el constructor ni `reconstitute(...)` (no hay campos nuevos).
- `reservations/domain/port/in/ApplyDiscountCommand.java` +
  `ApplyDiscountUseCase.java` (nuevos), mismo patrón que `ModifyReservationCommand`/
  `ModifyReservationUseCase`.
- `reservations/application/ApplyDiscountReservationService.java` (nuevo): valida
  tenant activo, busca la reserva, llama `applyDiscount()`, guarda, y registra el
  `AuditRecord` vía `AuditRecorder` (puerto ya existente en `common.audit`, se inyecta
  igual que en `DeactivateTenantService`).
- `reservations/infrastructure/in/web/dto/ApplyDiscountRequest.java` (nuevo): `record
  ApplyDiscountRequest(Integer percentage, String reason, String actorId)`.
- `reservations/infrastructure/in/web/ReservationController.java`: nuevo endpoint
  `POST /{reservationId}/apply-discount` + `@ExceptionHandler` para
  `ReservationNotDiscountableException` (409).
- `PLAN-VERIFICACION.md`: nueva sección "023".

## Decisiones técnicas

1. **Aplicaciones repetidas permitidas, sin bloqueo.** Alternativa descartada: exigir
   que solo se pueda aplicar una vez por reserva. Motivo: el Frontend no lo bloquea
   (`apply-discount.component.ts` no consulta historial previo) y la spec lo deja
   como fuera de alcance explícito.
2. **Trazabilidad vía `common/audit`, no campos nuevos en `Reservation`.** Alternativa
   descartada: agregar `discountPercentage`/`discountReason`/`discountedBy` a
   `Reservation` (como `cancellationReason`/`cancelledBy`), o una lista aparte tipo
   `companions`. Motivo: esos campos solo guardarían la ÚLTIMA aplicación (se pisan en
   cada llamada), perdiendo el historial que la propia spec pide conservar; una lista
   nueva duplicaría infraestructura que el proyecto ya tiene desde spec 002/021
   específicamente para esto. Evita además una migración nueva.
3. **`percentage` como `int` 1-100, sin `BigDecimal`.** Alternativa descartada:
   `BigDecimal percentage` para permitir decimales. Motivo: ni la spec ni el Frontend
   (`apply-discount.component.ts`, campo numérico entero) piden fracciones de
   porcentaje; se mantiene simple.

## Modelo de datos

Sin cambios. No hay migración nueva.

## Contratos

`POST /api/tenants/{tenantId}/reservations/{reservationId}/apply-discount`

Request:
```json
{ "percentage": 10, "reason": "Cliente frecuente", "actorId": "operador-1" }
```

Response `200`: mismo `ReservationResponse` ya existente (sin campos nuevos), con
`finalValue`/`pendingBalance`/`creditBalance`/`paymentStatus`/`refundDecisionStatus`
recalculados.

Errores:
- `400 validation_error`: `percentage` fuera de 1-100, o `reason` vacío.
- `409 reservation_not_discountable`: `reservationStatus` distinto de
  `PendienteDePago`/`Confirmada`.
- `404 not_found` / `409 tenant_inactive`: igual que el resto de endpoints de
  `reservations`.

## Cómo se verifica

- Criterio 1 (10% sin pagos, `200`): crear reserva, aplicar descuento, `GET` confirma
  `finalValue`/`pendingBalance` recalculados.
- Criterio 2 (saldo a favor): reserva `Confirmada` pagada por completo, aplicar
  descuento que deje `newFinalValue` por debajo de lo pagado, confirmar `creditBalance`/
  `paymentStatus`/`refundDecisionStatus`.
- Criterio 3 (`400`): `percentage` en 0, 101 y `reason` vacío.
- Criterio 4 (`409`): aplicar sobre una reserva `EnEjecucion`/`Cancelada` (reutilizar
  reservas de secciones anteriores, igual que sección "022").
- Criterio 5 (`404`/`409` tenant): igual patrón que las secciones anteriores.
- Criterio 6: `./mvnw test` en verde.

Todo documentado como `curl` en la nueva sección "023" de `PLAN-VERIFICACION.md`.
