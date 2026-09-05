# 017 — Plan técnico

## Enfoque

`Reservation` gana un método nuevo, `finalizeExecution(actorId)`, que transiciona su estado de
`EN_EJECUCION` a `FINALIZADA` — mismo patrón inmutable ya usado por `cancel()` (spec 011)
y `refund()` (spec 012): valida la precondición de estado, devuelve una instancia nueva,
lanza una excepción de dominio propia si no se cumple. La metadata de finalización
(`finalizedBy`, `finalizedAt`) se guarda como dos campos nuevos directamente en
`Reservation`, no en el registro `Execution` de `operations` (spec 010): sigue el mismo
criterio ya usado por cancelación/devolución (la metadata del estado vive en el agregado
que posee ese estado) y evita duplicar `reservationStatus` en dos sitios. El único ajuste
sobre `operations` es exponer esa metadata en la respuesta ya existente de `GET
.../execution`, cargando la `Reservation` asociada (el `OperationController` ya inyecta
`ReservationQueryUseCase`) en vez de guardar el dato dos veces.

## Cambios por repositorio

**Backend** (`hu-back-001-dev`):

- `reservations/domain/model/Reservation.java`: nuevos campos `finalizedBy`/`finalizedAt`
  + método `finalizeExecution(String actorId)`.
- `reservations/domain/exception/ReservationNotFinalizableException.java` (nuevo).
- `reservations/domain/port/in/FinalizeReservationCommand.java` +
  `FinalizeReservationUseCase.java` (nuevos).
- `reservations/application/FinalizeReservationService.java` (nuevo).
- `reservations/infrastructure/in/web/ReservationController.java`: nuevo endpoint
  `POST /{reservationId}/finalize` + manejo de `ReservationNotFinalizableException`.
- `reservations/infrastructure/in/web/dto/FinalizeReservationRequest.java` (nuevo).
- `reservations/infrastructure/in/web/dto/ReservationResponse.java`: agregar
  `finalizedBy`/`finalizedAt`.
- `reservations/infrastructure/out/persistence/ReservationEntity.java` +
  `ReservationRepositoryAdapter.java`: persistir/leer las dos columnas nuevas.
- `src/main/resources/db/migration/V14__add_reservation_finalization_fields.sql`
  (nuevo).
- `operations/infrastructure/in/web/dto/ExecutionResponse.java`: agregar
  `finalized`/`finalizedBy`/`finalizedAt`, derivados de la `Reservation` asociada.
- `operations/infrastructure/in/web/OperationController.java`: `getExecution` consulta
  también `reservationQueryUseCase.getById(...)` y pasa ambos a
  `ExecutionResponse.from(execution, reservation)`.
- `PLAN-VERIFICACION.md`: nueva sección "017".

Ningún cambio en Frontend ni Docs (regla de integración diferida, ya vigente). El Frontend
ya tiene la firma `finalizeExecution(code, actor)` lista para reemplazar su mecanismo
local por la llamada real cuando se decida esa integración (fuera de esta spec).

## Decisiones técnicas

1. **Metadata de finalización vive en `Reservation`, no en `Execution`**: resuelve la
   decisión abierta 1 de `spec.md`. Mismo criterio que cancelación/devolución
   (`cancellationReason`/`cancelledBy`/`cancelledAt`, `refundedAmount`/.../`refundedAt`
   ya viven en `Reservation`, no en un módulo aparte). Alternativa descartada: agregar
   `finalizedAt`/`finalizedBy` a `Execution` — obligaría a que `Execution` deje de ser
   inmutable-de-una-sola-escritura (hoy solo tiene `create`/`reconstitute`) y duplicaría
   el estado (`Reservation.reservationStatus` ya es la fuente única, decisión 4 de spec
   010).
2. **No se valida la existencia de `Execution` al finalizar**: resuelve la decisión
   abierta 2 de `spec.md`. `RegisterExecutionService.registerExecution()` es
   `@Transactional` y crea la fila de `Execution` en la misma transacción donde
   `Reservation` pasa a `EN_EJECUCION`; no existe ningún camino del código donde una
   reserva llegue a `EN_EJECUCION` sin tener su `Execution`. Se valida solo
   `reservation.reservationStatus() == EN_EJECUCION` (ya cargado para obtener
   `tenantId`/`reservationId`), sin una consulta adicional a `operations` ni una
   excepción nueva de "ejecución no encontrada" — mismo razonamiento que la decisión 4
   de spec 010 aplicado al revés (ahí se leía `Reservation` en vez de `Execution` para
   evitar redundancia; aquí es idéntico).
3. **`GET .../execution` expone la finalización cargando `Reservation` en el
   controlador, sin duplicar columnas**: `OperationController` ya inyecta
   `ReservationQueryUseCase` (lo usa en `listPendingExecution`); se reutiliza esa
   dependencia para pasar la `Reservation` a `ExecutionResponse.from(execution,
   reservation)`. Alternativa descartada: agregar `finalized_by`/`finalized_at` como
   columnas de `reservation_executions` — duplicaría una fuente de verdad que ya vive en
   `reservations`, con riesgo de que ambas tablas queden desincronizadas.
4. **Única excepción nueva, mismo patrón que `ReservationNotCancellableException` /
   `ReservationNotRefundableException`**: `ReservationNotFinalizableException` (409)
   cubre tanto "todavía no está en ejecución" como "ya se finalizó" (tras la primera
   finalización el estado deja de ser `EN_EJECUCION`), sin necesitar un segundo guard.

## Modelo de datos

```sql
ALTER TABLE reservations
    ADD COLUMN finalized_by VARCHAR(255),
    ADD COLUMN finalized_at TIMESTAMP;
```

## Contratos

- `POST /api/tenants/{tenantId}/reservations/{reservationId}/finalize` — body
  `{ "actorId": string }` → `200` con `ReservationResponse` (incluye `finalizedBy`,
  `finalizedAt`). `404` tenant o reserva inexistente. `409` tenant `Inactivo`, o reserva
  que no está `En ejecucion` (ya `Confirmada`, `Pendiente de pago`, `Cancelada` o ya
  `Finalizada`) — `reservation_not_finalizable`.
- `GET /api/tenants/{tenantId}/reservations/{reservationId}/execution` (spec 010, sin
  cambio de ruta) → `200` con `ExecutionResponse` extendido: `{ reservationId, served,
  executed, causal, actorId, recordedAt, finalized, finalizedBy, finalizedAt }`.
  `finalized` es `true` solo si `reservationStatus == FINALIZADA`; `finalizedBy`/
  `finalizedAt` son `null` mientras no se haya finalizado.

Sin JWT (mismo criterio que specs 009/010/011): operaciones de operador/staff, sin login
todavía.

## Cómo se verifica

Un `curl` por criterio de aceptación, en la nueva sección "017" de
`PLAN-VERIFICACION.md`: finalizar una ejecución en curso, rechazo por reserva
`Pendiente de pago`/`Confirmada`/`Cancelada`/ya `Finalizada` (409), doble finalización
(409), `GET` de la ejecución antes y después de finalizar (verificando que `finalized`
cambia y el resto de campos de spec 010 no se alteran), verificar que
`finalValue`/`pendingBalance`/`creditBalance`/`paymentStatus` no cambian tras finalizar,
tenant inexistente (404) / inactivo (409), y `./mvnw test`.
