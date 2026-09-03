# 005 — Tareas

**Batching (excepción regla 4 de `CLAUDE.md` — Backend, implementación inicial):**
lotes de 2-3 tareas, commit + push sin pedir permiso en cada uno, siempre que
`PLAN-VERIFICACION.md` esté al día en el repo al momento del push.

- [x] T01 — Migración `V4__create_catalog_items.sql` (tabla `catalog_items` + índice por tenant) · repo: backend · ~15 min
- [x] T02 — `CatalogItemType` (enum), `CatalogItem` (aggregate: `create`, `reconstitute`, `update`, `deactivate`, `reactivate`, validación de capacidad obligatoria y positiva solo para `LODGING`) · repo: backend · ~25 min · depende de T01
- [x] T03 — `InvalidCatalogItemException`, `CatalogItemNotFoundException` (dominio) · repo: backend · ~10 min
  **— fin lote 1 (T01-T03): commit + push —**
- [x] T04 — Puertos de entrada (`CreateCatalogItemCommand/UseCase`, `UpdateCatalogItemCommand/UseCase`, `DeactivateCatalogItemUseCase`, `ReactivateCatalogItemUseCase`, `CatalogItemQueryUseCase`) y de salida (`CatalogItemRepositoryPort`) · repo: backend · ~20 min · depende de T02, T03
- [x] T05 — `CatalogItemEntity`, `CatalogItemJpaRepository`, `CatalogItemRepositoryAdapter` (mapeo hacia/desde el dominio vía `CatalogItem.reconstitute`) · repo: backend · ~20 min · depende de T04
  **— fin lote 2 (T04-T05): commit + push —**
- [x] T06 — `CreateCatalogItemService` y `CatalogItemQueryService` (resuelven tenant vía `TenantRepositoryPort`, rechazan tenant inexistente/`Inactivo` igual que `RegisterCustomerService`) · repo: backend · ~20 min · depende de T05
- [x] T07 — `UpdateCatalogItemService`, `DeactivateCatalogItemService`, `ReactivateCatalogItemService` · repo: backend · ~20 min · depende de T06
  **— fin lote 3 (T06-T07): commit + push —**
- [x] T08 — `CatalogItemController` (`POST/GET/PATCH` + `/deactivate`/`/reactivate` bajo `/api/tenants/{tenantId}/catalog-items`), DTOs `CatalogItemRequest`/`CatalogItemPatchRequest`/`CatalogItemResponse`, manejadores de excepción (`400`/`404`/`409`) · repo: backend · ~30 min · depende de T07
  **— fin lote 4 (T08): commit + push —**
- [x] T09 — Agrega la sección "005 — Catálogo operativo" a `PLAN-VERIFICACION.md` con los `curl` de cada criterio de aceptación · repo: backend · ~20 min · depende de T08
- [x] T10 — Verifica que `./mvnw test` sigue en verde (spec 001+002+003+004+005) y ejecuta la sección nueva de `PLAN-VERIFICACION.md` de punta a punta contra el servidor local · repo: backend · ~25 min · depende de T09
  **— fin lote 5 (T09-T10): commit + push —**
