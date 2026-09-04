# 010 — Tareas

- [x] T01 — Agregar `Reservation.startExecution()` +
      `ReservationNotExecutableException` + método `listPendingExecutionByTenant` en
      `ReservationQueryUseCase`/`ReservationQueryService` · repo: backend · ~20 min
- [x] T02 — Migración `V8__create_operations_tables.sql` (`reservation_executions`,
      `operation_costs`) + modelos de dominio `Execution`/`OperationCost`
      (`create`/`reconstitute`, validaciones) en nuevo módulo `operations` · repo:
      backend · depende de T01 · ~25 min

  *(fin lote 1: T01-T02)*

- [x] T03 — Puertos out (`ExecutionRepositoryPort`, `OperationCostRepositoryPort`) +
      entidades JPA + adapters (`ExecutionEntity`/`OperationCostEntity`, repos Spring
      Data, adapters) · repo: backend · depende de T02 · ~25 min
- [x] T04 — Puertos in + servicios: `RegisterExecutionCommand`/`UseCase`/`Service`
      (usa `Reservation.startExecution()`, guarda `Reservation` + `Execution`) y
      `ExecutionQueryUseCase`/`Service` (consultar ejecución de una reserva,
      `ExecutionNotFoundException`) · repo: backend · depende de T03 · ~30 min

  *(fin lote 2: T03-T04)*

- [x] T05 — Puertos in + servicios: `RegisterOperationCostCommand`/`UseCase`/`Service`
      (exige `EN_EJECUCION`, nueva `ExecutionNotStartedException`) y
      `OperationCostQueryUseCase`/`Service` (listar costos por reserva, orden
      cronológico) · repo: backend · depende de T04 · ~25 min
- [x] T06 — `OperationController` (endpoints `POST`/`GET .../execution`,
      `GET /reservations/pending-execution`, `POST`/`GET .../costs`) + DTOs
      request/response + manejo de excepciones (400/404/409) · repo: backend ·
      depende de T05 · ~30 min

  *(fin lote 3: T05-T06)*

- [x] T07 — Actualizar `PLAN-VERIFICACION.md` con la sección "010 — Ejecución de
      reservas y costos operacionales": un `curl` por cada criterio de aceptación ·
      repo: backend · depende de T06 · ~20 min

- [ ] T08 — Verificación final: `./mvnw test` en verde y ejecución manual de
      `PLAN-VERIFICACION.md` sección 010 contra el servidor local; marcar los
      criterios de aceptación de `spec.md` como cumplidos · repo: backend · depende de
      T07 · requiere permiso explícito para build/tests/servidor (regla 5 de
      CLAUDE.md) · ~25 min
