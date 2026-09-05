# 015 — Tareas

- [x] T01 — Agregar `TRANSPORT` a `CatalogItemType` y actualizar el comentario de
      clase de `CatalogItem` que lo excluye  · repo: backend · ~5 min
- [x] T02 — Agregar `route`/`operationalCost` a `CatalogItem` (constructor, `create`,
      `reconstitute`, `update`, getters)  · repo: backend · ~20 min · depende de T01
- [x] T03 — Agregar `route`/`operationalCost` a `CreateCatalogItemCommand` y
      `UpdateCatalogItemCommand`, y propagarlos en `CreateCatalogItemService`/
      `UpdateCatalogItemService` (ajuste sobre el plan: sus llamadas a
      `CatalogItem.create/update` son posicionales y habrían roto la compilación)
      · repo: backend · ~10 min · depende de T02
- [x] T04 — Agregar `route`/`operationalCost` a `CatalogItemRequest`,
      `CatalogItemPatchRequest`, `CatalogItemResponse`  · repo: backend · ~10 min ·
      depende de T02
- [x] T05 — `CatalogItemController`: pasar los dos campos nuevos en `create()` y
      `update()`  · repo: backend · ~10 min · depende de T03, T04
- [x] T06 — Migración `V13__add_transport_fields_to_catalog_items.sql` +
      `CatalogItemEntity` (columnas, constructor, getters)  · repo: backend · ~15 min
      · depende de T02
- [x] T07 — `CatalogItemRepositoryAdapter`: pasar los dos campos nuevos en `save()`
      y `toDomain()`  · repo: backend · ~10 min · depende de T06
- [x] T08 — Verificar los criterios de aceptación de la spec: `./mvnw test` +
      secuencia curl completa (creación sin opcionales, round-trip route/cost,
      PATCH parcial, regresión LODGING/capacity, aislamiento/soft-delete/tenant
      404-409 con TRANSPORT) + `PLAN-VERIFICACION.md`  · repo: backend · ~25 min ·
      depende de T05, T07
