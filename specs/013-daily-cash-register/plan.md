# 013 — Plan técnico

## Enfoque

Nuevo módulo `cash`, mismo patrón hexagonal que `operations`/`reservations`: un agregado
`CashRegister` (una caja por `tenantId` + `businessDate`) con `CashMovement` y
`CashCorrection` como hijos embebidos, persistidos en tablas propias con FK. Las
devoluciones (`DEVOLUCION`) **no se persisten como `CashMovement`**: se calculan en vivo
consultando `reservations` (reservas con `refundedAt` no nulo en ese `tenantId` +
`businessDate`), igual que hace el Frontend real con `getExecutedRefundMovements()`. La
consolidación mensual agrega los cierres propios de `cash` más dos lecturas cruzadas de
solo lectura: cancelaciones (`reservations`) y costos operacionales (`operations`).

Dirección de dependencia: `cash` inyecta `ReservationRepositoryPort` (de `reservations`)
y `OperationCostRepositoryPort` (de `operations`) directamente por constructor — mismo
patrón ya usado por `RefundReservationService` (en `reservations`) inyectando
`TenantRepositoryPort` (de `tenants`). Ningún módulo existente pasa a depender de `cash`.

## Cambios por repositorio

**Backend** (`com.corhuila.errorcapa8.travesia_natural.cash`, rama `hu-back-001-dev`):

- `domain/model/`: `CashRegister` (agregado), `CashMovement`, `CashCorrection`,
  `CashRegisterStatus` (`ABIERTA`/`CERRADA`), `CashMovementType` (`INGRESO`/`PAGO`/
  `GASTO` — `DEVOLUCION` no es un valor persistible, se maneja aparte como campo derivado
  en el DTO de respuesta).
- `domain/exception/`: `CashRegisterNotFoundException` (404),
  `CashRegisterAlreadyOpenException` (409, apertura duplicada),
  `CashRegisterClosedException` (409, movimiento/cierre sobre caja ya cerrada). Los `400`
  de validación reutilizan `IllegalArgumentException` genérica, igual que
  `OperationController`.
- `domain/port/in/`: `OpenCashRegisterUseCase`+`Command`,
  `RegisterCashMovementUseCase`+`Command`, `CloseCashRegisterUseCase`+`Command`,
  `AddCashCorrectionUseCase`+`Command`, `CashRegisterQueryUseCase`,
  `MonthlyCashConsolidationQueryUseCase`.
- `domain/port/out/`: `CashRegisterRepositoryPort` (save, `findByTenantIdAndBusinessDate`,
  `findAllClosedByTenantId`, `findAllClosedByTenantIdAndPeriod`).
- `infrastructure/out/persistence/`: `CashRegisterEntity` + `CashMovementEntity` +
  `CashCorrectionEntity` (JPA, FK a `cash_registers`), `CashRegisterJpaRepository`,
  `CashRegisterRepositoryAdapter`.
- `infrastructure/in/web/`: `CashController` (`/api/tenants/{tenantId}/cash`) + DTOs
  (`OpenCashRegisterRequest`, `RegisterCashMovementRequest`, `CloseCashRegisterRequest`,
  `AddCashCorrectionRequest`, `CashRegisterResponse`, `CashMovementResponse`,
  `MonthlyConsolidationResponse`) + `@ExceptionHandler` locales (mismo patrón que
  `OperationController`, sin `@ControllerAdvice` global — no existe en el proyecto).
- `application/`: `OpenCashRegisterService`, `RegisterCashMovementService`,
  `CloseCashRegisterService`, `AddCashCorrectionService`, `CashRegisterQueryService`,
  `MonthlyCashConsolidationService`.
- `operations/domain/port/out/OperationCostRepositoryPort.java`: se agrega
  `findAllByTenantId(String tenantId)` (hoy solo existe `findAllByTenantIdAndReservationId`)
  + implementación en `OperationCostRepositoryAdapter`. Único cambio fuera del paquete
  `cash`.
- `src/main/resources/db/migration/V12__create_cash_tables.sql`.

**Frontend**: ninguno — ya construido (spec 013 documenta alineación, no genera trabajo
de Frontend).

## Decisiones técnicas

1. **Un agregado, no dos**: `CashRegister` con `List<CashMovement>` y
   `List<CashCorrection>` internos, en vez de agregados separados. Alternativa
   descartada: `CashMovement` como agregado propio — se descarta porque nunca se
   consulta/modifica un movimiento fuera del contexto de su caja (mismo criterio que
   `Reservation`/`ReservedService`).
2. **`DEVOLUCION` calculado en vivo, no escrito como evento**: alternativa descartada,
   escribir un `CashMovement` real al ejecutar `RefundReservationService.refundReservation()`.
   Se descarta por acoplar `reservations` a `cash` (dirección de dependencia invertida,
   contra el patrón establecido) y por replicar innecesariamente un dato ya persistido en
   `Reservation.refundedAt/refundedAmount`. La consulta en vivo es una lectura simple
   (`findAllByTenantId` + filtro en memoria) sobre un volumen académico, sin necesidad de
   índice ni query dedicada.
3. **`businessDate` como `LocalDate`, comparado contra `Instant` truncando a UTC**: el
   backend no maneja zona horaria de tenant en ningún otro módulo (a diferencia del
   Frontend, que sí la introdujo para Caja). Se mantiene el mismo criterio simple del
   resto del proyecto (`Instant.atZone(ZoneOffset.UTC).toLocalDate()`) en vez de
   introducir manejo de zona horaria nuevo solo para este módulo. Riesgo aceptado y
   documentado, no bloqueante para la materia.
4. **`UNIQUE(tenant_id, business_date)` en `cash_registers`**: constraint de base de datos
   más validación de aplicación (doble capa), resolviendo la decisión abierta #3 de la
   spec. Solo puede existir una caja — abierta o cerrada — por tenant y fecha; no se
   reabre una cerrada (las correcciones cubren ese caso).
5. **`OperationCostRepositoryPort` gana un método nuevo en vez de que `cash` reimplemente
   la consulta**: mantiene la lectura de costos operacionales en su módulo dueño
   (`operations`), mismo criterio de propiedad de datos que el resto del proyecto.

## Modelo de datos

`V12__create_cash_tables.sql`:

```sql
CREATE TABLE cash_registers (
    cash_register_id UUID PRIMARY KEY,
    tenant_id VARCHAR NOT NULL REFERENCES tenants(tenant_id),
    business_date DATE NOT NULL,
    base_amount NUMERIC NOT NULL,
    status VARCHAR NOT NULL,
    closed_by VARCHAR,
    closed_at TIMESTAMP,
    total_amount NUMERIC,
    UNIQUE (tenant_id, business_date)
);

CREATE TABLE cash_movements (
    movement_id UUID PRIMARY KEY,
    cash_register_id UUID NOT NULL REFERENCES cash_registers(cash_register_id),
    type VARCHAR NOT NULL,
    amount NUMERIC NOT NULL,
    concept VARCHAR NOT NULL,
    actor_id VARCHAR NOT NULL,
    recorded_at TIMESTAMP NOT NULL
);

CREATE TABLE cash_corrections (
    correction_id UUID PRIMARY KEY,
    cash_register_id UUID NOT NULL REFERENCES cash_registers(cash_register_id),
    justification VARCHAR NOT NULL,
    applied_by VARCHAR NOT NULL,
    applied_at TIMESTAMP NOT NULL
);
```

## Contratos

Base: `/api/tenants/{tenantId}/cash`

- `POST /` — abrir caja. Body: `{businessDate, baseAmount, actorId}` → `201`
  `CashRegisterResponse`. `409` si ya hay una `ABIERTA` o `CERRADA` para esa fecha.
- `POST /{cashRegisterId}/movements` — Body: `{type, amount, concept, actorId}`, `type`
  ∈ `{INGRESO, PAGO, GASTO}`. → `201` `CashRegisterResponse` (estado + total
  actualizados). `400` si `type=DEVOLUCION` o cualquier campo inválido. `409` si la caja
  está `CERRADA`.
- `POST /{cashRegisterId}/close` — Body: `{actorId}` → `200` `CashRegisterResponse` con
  `totalAmount` congelado. `409` si ya estaba `CERRADA`.
- `GET /?businessDate=YYYY-MM-DD` — → `200` `CashRegisterResponse` (movimientos propios +
  devoluciones calculadas en vivo, total en vivo si `ABIERTA`).
- `GET /history` — → `200` `List<CashRegisterResponse>` (solo `CERRADA`s, orden
  descendente).
- `POST /{cashRegisterId}/corrections` — Body: `{justification, actorId}` → `201`
  `CashRegisterResponse` con la corrección agregada. `409` si la caja sigue `ABIERTA`
  (corrección es solo post-cierre).
- `GET /consolidation?period=YYYY-MM` — → `200` `List<MonthlyConsolidationResponse>`
  (`{period, ingresos, pagosOperacionales, gastos, devoluciones, total, cancelaciones,
  costosOperacionales}`).

Todos: `404` si `tenantId` no existe, `409` si `Inactivo`.

## Cómo se verifica

`curl` contra cada endpoint, en este orden, reproduce los criterios de aceptación de la
spec: abrir caja → duplicado devuelve `409` → registrar `INGRESO`/`PAGO`/`GASTO` (verificar
`400` con `DEVOLUCION`) → cerrar → cerrar de nuevo devuelve `409` → consultar caja (total
congelado) → consultar histórico → ejecutar una devolución sobre una reserva de ese
tenant/fecha (spec 012) y confirmar que aparece reflejada sin registrarla manualmente →
registrar corrección sobre la caja cerrada → consultar consolidación mensual y verificar
los 7 valores agregados. Se documenta en `PLAN-VERIFICACION.md` junto al commit, mismo
criterio que specs 009-012. Además: `mvn test` (o el comando de build correspondiente)
debe seguir compilando y pasando specs 001-012.
