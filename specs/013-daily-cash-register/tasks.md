# 013 — Tareas

- [x] T01 — Migración `V12__create_cash_tables.sql` (`cash_registers`, `cash_movements`,
      `cash_corrections`, `UNIQUE(tenant_id, business_date)`)  · repo: backend · ~20 min
- [x] T02 — Modelo de dominio `CashRegister` + `CashMovement` + `CashCorrection` +
      `CashRegisterStatus`/`CashMovementType`, con métodos `open`/`registerMovement`/
      `close`/`addCorrection` sobre el agregado  · repo: backend · ~30 min
- [x] T03 — Excepciones de dominio `CashRegisterNotFoundException`,
      `CashRegisterAlreadyOpenException`, `CashRegisterClosedException`,
      `CashRegisterNotClosedException` (esta última surgió durante la implementación: el
      plan no la anticipó, necesaria para el `409` de corrección sobre caja `ABIERTA`)
      · repo: backend · ~15 min · depende de T02
- [x] T04 — Puertos `in`: `OpenCashRegisterUseCase`+`Command`,
      `RegisterCashMovementUseCase`+`Command`, `CloseCashRegisterUseCase`+`Command`,
      `AddCashCorrectionUseCase`+`Command`, `CashRegisterQueryUseCase`,
      `MonthlyCashConsolidationQueryUseCase`  · repo: backend · ~20 min · depende de T02
- [x] T05 — Puerto `out`: `CashRegisterRepositoryPort`
      · repo: backend · ~10 min · depende de T02
- [x] T06 — Infraestructura de persistencia: `CashRegisterEntity`/`CashMovementEntity`/
      `CashCorrectionEntity`, `CashRegisterJpaRepository`, `CashRegisterRepositoryAdapter`
      (mapeo agregado ↔ entidades)  · repo: backend · ~30 min · depende de T01, T05
- [x] T07 — `OperationCostRepositoryPort.findAllByTenantId(String)` + implementación en
      `OperationCostRepositoryAdapter`  · repo: backend · ~15 min
- [x] T08 — `OpenCashRegisterService` (tenant activo, rechaza apertura duplicada)
      · repo: backend · ~20 min · depende de T03, T04, T06
- [x] T09 — `RegisterCashMovementService` (rechaza `DEVOLUCION` con
      `IllegalArgumentException`, valida caja `ABIERTA`)
      · repo: backend · ~20 min · depende de T03, T04, T06
- [x] T10 — `CloseCashRegisterService` (calcula `totalAmount` con movimientos propios +
      devoluciones en vivo vía `ReservationRepositoryPort`). Nota: la lectura de
      devoluciones se extrajo a `RefundsTotalCalculator` (no planeado explícitamente en
      plan.md), compartido con T12 para no duplicar el filtro `refundedAt` por
      `businessDate` · repo: backend · ~25 min · depende de T03, T04, T06
- [x] T11 — `AddCashCorrectionService` (solo sobre caja `CERRADA`)
      · repo: backend · ~15 min · depende de T03, T04, T06
- [x] T12 — `CashRegisterQueryService` (consulta por fecha con total en vivo si `ABIERTA`,
      congelado si `CERRADA`; listado de histórico)
      · repo: backend · ~25 min · depende de T04, T06
- [x] T13 — `MonthlyCashConsolidationService` (agrupa cierres del periodo + cancelaciones
      vía `ReservationRepositoryPort` + costos operacionales vía
      `OperationCostRepositoryPort.findAllByTenantId`). Nota: `total` = ingresos - pagos -
      gastos - devoluciones, sin sumar `baseAmount` (RN-CAJ-001: "sin sumar repetidamente
      cada base diaria") · repo: backend · ~25 min · depende de T04, T06, T07
- [x] T14 — DTOs de infraestructura web (`*Request`/`*Response`,
      `MonthlyConsolidationResponse`)  · repo: backend · ~20 min · depende de T02
- [x] T15 — `CashController` con los 7 endpoints y `@ExceptionHandler` locales (mismo
      patrón que `OperationController`). Nota: se agregó `TenantGuard` (no planeado
      explícitamente) para aplicar el 404/409 de tenant de forma uniforme en las 6
      operaciones de aplicación (spec: "Todas las operaciones rechazan tenant inexistente
      o Inactivo", no solo apertura) · repo: backend · ~30 min · depende de T08, T09,
      T10, T11, T12, T13, T14
- [x] T16 — Verificar los criterios de aceptación de la spec 013 end-to-end (`curl` +
      `mvn test`), redactar `PLAN-VERIFICACION.md`. Verificado en vivo contra la BD real
      de desarrollo: la integración cruzada `cash` → `reservations` (total en vivo de una
      caja `ABIERTA` descontando devoluciones reales de spec 012) quedó confirmada con
      datos reales, no simulados · repo: backend · ~20 min · depende de T15
