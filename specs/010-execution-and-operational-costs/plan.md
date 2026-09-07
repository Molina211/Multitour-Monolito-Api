# 010 — Plan técnico

## Enfoque

`Reservation` gana un único método nuevo, `startExecution()`, que transiciona su estado
de `CONFIRMADA` a `EN_EJECUCION` (mismo patrón inmutable ya usado en spec 009: devuelve
una instancia nueva, lanza excepción si el estado actual no es `CONFIRMADA` — lo que
cubre a la vez "nunca se confirmó" y "ya se está ejecutando"). El detalle de la
ejecución (prestado/no prestado, cantidad, causal) y los costos operacionales no se
modelan como más campos sobre `Reservation` — a diferencia del pago, no son un valor
único que se sobrescribe sino registros propios con su propia tabla (una ejecución por
reserva, N costos por reserva) — así que viven en un bounded context nuevo,
`operations`, que solo lee `Reservation` a través del puerto ya existente
(`ReservationRepositoryPort`) para transicionar su estado y consultar `reservationId`/
`tenantId`. Todo el resto (tenant activo, 404/409, exceptions por controlador) sigue el
mismo patrón que specs 002/007/009.

## Cambios por repositorio

**Backend** (`hu-back-001-dev`):

- `reservations/domain/model/Reservation.java`: nuevo método `startExecution()`.
- `reservations/domain/exception/ReservationNotExecutableException.java` (nuevo).
- `reservations/domain/port/in/ReservationQueryUseCase.java`: nuevo método
  `listPendingExecutionByTenant(String tenantId)`.
- `reservations/application/ReservationQueryService.java`: implementa el método nuevo.
- `operations/domain/model/Execution.java` (nuevo).
- `operations/domain/model/OperationCost.java` (nuevo).
- `operations/domain/exception/ExecutionNotFoundException.java` (nuevo).
- `operations/domain/exception/ExecutionNotStartedException.java` (nuevo).
- `operations/domain/port/out/ExecutionRepositoryPort.java` (nuevo).
- `operations/domain/port/out/OperationCostRepositoryPort.java` (nuevo).
- `operations/domain/port/in/RegisterExecutionCommand.java` +
  `RegisterExecutionUseCase.java` (nuevos).
- `operations/domain/port/in/ExecutionQueryUseCase.java` (nuevo).
- `operations/domain/port/in/RegisterOperationCostCommand.java` +
  `RegisterOperationCostUseCase.java` (nuevos).
- `operations/domain/port/in/OperationCostQueryUseCase.java` (nuevo).
- `operations/application/RegisterExecutionService.java` (nuevo).
- `operations/application/ExecutionQueryService.java` (nuevo).
- `operations/application/RegisterOperationCostService.java` (nuevo).
- `operations/application/OperationCostQueryService.java` (nuevo).
- `operations/infrastructure/out/persistence/ExecutionEntity.java` +
  `ExecutionJpaRepository.java` + `ExecutionRepositoryAdapter.java` (nuevos).
- `operations/infrastructure/out/persistence/OperationCostEntity.java` +
  `OperationCostJpaRepository.java` + `OperationCostRepositoryAdapter.java` (nuevos).
- `operations/infrastructure/in/web/OperationController.java` (nuevo) + DTOs
  (`RegisterExecutionRequest`, `ExecutionResponse`, `RegisterOperationCostRequest`,
  `OperationCostResponse`).
- `src/main/resources/db/migration/V8__create_operations_tables.sql` (nuevo).
- `PLAN-VERIFICACION.md`: nueva sección "010".

Ningún cambio en Frontend ni Docs (regla de integración diferida, ya vigente desde
sesiones anteriores).

## Decisiones técnicas

1. **Un solo estado por reserva cubre "no confirmada" y "ya ejecutándose"**: en vez de
   dos excepciones (`ReservationNotConfirmedException` /
   `ExecutionAlreadyRegisteredException`), `Reservation.startExecution()` lanza una
   única `ReservationNotExecutableException` (409) cuando `reservationStatus !=
   CONFIRMADA`. Motivo: tras la primera ejecución la reserva ya está en
   `EN_EJECUCION`, así que un segundo intento cae en el mismo guard sin necesitar una
   tabla de ejecuciones para detectarlo — resuelve la decisión abierta 1 de `spec.md` a
   favor de `409` (conflicto de estado, mismo criterio que
   `PaymentAlreadyResolvedException` en spec 009), y descarta la alternativa de usar
   `400` porque el problema no es el contenido de la petición sino el estado actual del
   recurso.
2. **Bounded context nuevo `operations`, no más campos en `Reservation`**: resuelve la
   decisión abierta 2 de `spec.md`. Alternativa descartada: extender `Reservation` como
   hizo spec 009 con el pago — no encaja porque un costo operacional es una lista que
   crece (N por reserva), no un valor que se sobrescribe, y forzarlo en el agregado
   inmutable de `Reservation` obligaría a cargar toda la lista de costos en cada
   operación sobre la reserva sin necesidad.
3. **No se guarda ningún conteo de "personas reservadas"**: resuelve la decisión
   abierta 3 de `spec.md`. `Execution` solo guarda `served`, `executed`, `causal`,
   `actorId`, `recordedAt` — nada de un campo `reserved`. Motivo: ningún criterio de
   aceptación exige validar `executed` contra un tope, y `Reservation` no tiene hoy un
   conteo total de personas del que derivarlo sin inventar una fórmula (spec, riesgo 3).
4. **Costo operacional exige `EN_EJECUCION`, verificado contra
   `Reservation.reservationStatus`, no contra la existencia de una fila en
   `reservation_executions`**: como el único camino para llegar a `EN_EJECUCION` es
   `startExecution()`, y `startExecution()` es lo único que crea la fila de ejecución,
   ambas condiciones son equivalentes; se prefiere leer el estado de `Reservation`
   (ya cargado para obtener `tenantId`/`reservationId`) en vez de una consulta
   adicional a `reservation_executions`. Nueva excepción
   `ExecutionNotStartedException` (409), en `operations` (no en `reservations`, porque
   es una regla del caso de uso "registrar costo", no un invariante de `Reservation`).
5. **Validaciones de entrada (causal obligatoria si no se prestó, concepto no vacío,
   monto positivo) lanzan `IllegalArgumentException`**, no una excepción de dominio
   nueva — mismo criterio que `ReservedService` (spec 001): son invariantes de
   construcción del propio value object (`Execution.create`/`OperationCost.create`),
   no reglas de aplicación. El `@ExceptionHandler(IllegalArgumentException.class)` en
   `OperationController` ya sigue el mismo patrón que `PaymentController`.

## Modelo de datos

```sql
CREATE TABLE reservation_executions (
    execution_id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    reservation_id UUID NOT NULL UNIQUE REFERENCES reservations(reservation_id),
    served BOOLEAN NOT NULL,
    executed INTEGER NOT NULL,
    causal VARCHAR(500),
    actor_id VARCHAR(255),
    recorded_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE operation_costs (
    cost_id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    reservation_id UUID NOT NULL REFERENCES reservations(reservation_id),
    concept VARCHAR(255) NOT NULL,
    amount NUMERIC(14, 2) NOT NULL,
    actor_id VARCHAR(255),
    recorded_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_operation_costs_reservation ON operation_costs(reservation_id);
```

`UNIQUE` sobre `reservation_id` en `reservation_executions` es solo una salvaguarda de
base de datos: la regla real ("no se puede ejecutar dos veces") ya la aplica
`Reservation.startExecution()` antes de llegar a guardar.

## Contratos

- `POST /api/tenants/{tenantId}/reservations/{reservationId}/execution` — body
  `{ "served": boolean, "executed": number (solo si served=true, opcional), "causal": string (obligatorio si served=false), "actorId": string }`
  → `200` con `ExecutionResponse`. `400` si `served=false` sin `causal`. `404` tenant o
  reserva inexistente. `409` tenant `Inactivo`, o reserva no `Confirmada`/ya con
  ejecución registrada.
- `GET /api/tenants/{tenantId}/reservations/{reservationId}/execution` → `200` con
  `ExecutionResponse` si existe, `404` (`execution_not_found`) si la reserva no tiene
  ejecución registrada.
- `GET /api/tenants/{tenantId}/reservations/pending-execution` → `200` con
  `ReservationResponse[]` (reservas `Confirmada` de ese tenant sin ejecución).
- `POST /api/tenants/{tenantId}/reservations/{reservationId}/costs` — body
  `{ "concept": string, "amount": number, "actorId": string }` → `201` con
  `OperationCostResponse`. `400` si `concept` vacío o `amount <= 0`. `404` tenant o
  reserva inexistente. `409` tenant `Inactivo`, o reserva sin ejecución iniciada
  (`execution_not_started`).
- `GET /api/tenants/{tenantId}/reservations/{reservationId}/costs` → `200` con
  `OperationCostResponse[]` en orden cronológico (puede ser `[]`).

`ExecutionResponse`: `{ reservationId, served, executed, causal, actorId, recordedAt }`.
`OperationCostResponse`: `{ costId, reservationId, concept, amount, actorId, recordedAt }`.

Sin JWT (mismo criterio que spec 009, decisión técnica 3): operaciones de
operador/staff, sin login todavía.

## Cómo se verifica

Un `curl` por criterio de aceptación, igual que specs anteriores, en la nueva sección
"010" de `PLAN-VERIFICACION.md`: registrar ejecución prestada, registrar ejecución no
prestada sin/con causal, registrar sobre reserva no confirmada (409), registrar
ejecución dos veces (409), `GET` de la ejecución, `GET` de pendientes de ejecución
(aislado por tenant), registrar costo sin ejecución (409), registrar costo con
concepto/monto inválido (400), registrar costo válido y un segundo costo acumulado,
`GET` de costos, tenant inexistente (404) / inactivo (409), y `./mvnw test`.
