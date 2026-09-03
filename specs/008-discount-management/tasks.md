# 008 — Tareas

**Batching (excepción regla 4 de `CLAUDE.md` — Backend, implementación inicial):**
lotes de 2-3 tareas, commit + push sin pedir permiso en cada uno, siempre que
`PLAN-VERIFICACION.md` esté al día en el repo al momento del push. Aplica igual en la
rama `hu-back-001-dev` (es donde vive el trabajo diario de esta fase, por decisión
explícita del usuario del 2026-09-03). La ejecución de build/tests/servidor sigue
pidiendo permiso cada vez (regla 5) — el batching cubre `commit`/`push`, no la
ejecución local.

- [ ] T01 — `Discount` (agregado) + `DiscountBase` (enum) + `InvalidDiscountException` +
  `DiscountNotFoundException` · repo: backend · ~25 min
- [ ] T02 — `V6__create_discounts.sql` + `DiscountEntity` + `DiscountJpaRepository` +
  `DiscountRepositoryAdapter` + `DiscountRepositoryPort` · repo: backend · ~25 min ·
  depende de T01
  **— fin lote (T01-T02): commit + push —**
- [ ] T03 — `CreateDiscountCommand`/`CreateDiscountUseCase`/`CreateDiscountService`
  (valida tenant existe/activo vía `TenantRepositoryPort`, y `catalogItemId` existe en
  ese tenant vía `CatalogItemRepositoryPort`) · repo: backend · ~20 min · depende de T02
- [ ] T04 — `DiscountQueryUseCase`/`DiscountQueryService` (listByTenant, getById) +
  `DiscountController` (`POST`, `GET` lista, `GET` por id) + `DiscountRequest`/
  `DiscountResponse` + `@ExceptionHandler` (400/404/409, reutilizando excepciones de
  `tenants`/`catalog`) · repo: backend · ~30 min · depende de T03
  **— fin lote (T03-T04): commit + push —**
- [ ] T05 — `UpdateDiscountCommand`/`UpdateDiscountUseCase`/`UpdateDiscountService` +
  endpoint `PATCH` + `DiscountPatchRequest` · repo: backend · ~20 min · depende de T04
- [ ] T06 — `DeactivateDiscountUseCase`/`ReactivateDiscountUseCase` + servicios +
  endpoints `POST .../deactivate` y `.../reactivate` · repo: backend · ~20 min ·
  depende de T05
  **— fin lote (T05-T06): commit + push —**
- [ ] T07 — Agrega la sección "008 — Gestión de descuentos" a `PLAN-VERIFICACION.md` con
  el `curl` de cada criterio de aceptación, incluyendo el caso de solape permitido ·
  repo: backend · ~15 min · depende de T06
- [ ] T08 — Verifica que `./mvnw test` sigue en verde y ejecuta la sección nueva de
  `PLAN-VERIFICACION.md` de punta a punta contra el servidor local (pide permiso antes
  de correr build/tests/servidor, regla 5) · repo: backend · ~20 min · depende de T07
  **— fin lote (T07-T08): commit + push —**
