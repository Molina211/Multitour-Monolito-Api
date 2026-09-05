# 015 — Plan técnico

## Enfoque

`CatalogItem` ya es un único agregado discriminado por `type` (TOUR/LODGING/FOOD).
Se agrega `TRANSPORT` al enum y dos campos opcionales al agregado (`route`,
`operationalCost`), siguiendo exactamente el mismo patrón que `capacity`: un campo
nullable en toda la cadena (dominio → entidad JPA → DTOs), sin validación obligatoria
salvo la ya existente de `capacity` para `LODGING`. No se toca el controller (mismo
`CatalogItemRequest`/`PatchRequest`, solo con dos campos más), no se agregan
endpoints nuevos.

## Cambios por repositorio

**Backend**, módulo `catalog`:

- `domain/model/CatalogItemType.java` — agregar `TRANSPORT`; actualizar el comentario
  de clase de `CatalogItem.java` que hoy dice "Transport... is out of scope".
- `domain/model/CatalogItem.java` — agregar `route` (String) y `operationalCost`
  (BigDecimal) al constructor, a `create()`, `reconstitute()`, `update()` (con el
  mismo patrón `campo != null ? campo : this.campo` que ya usan los demás opcionales)
  y sus getters.
- `domain/port/in/CreateCatalogItemCommand.java` / `UpdateCatalogItemCommand.java` —
  agregar `route`, `operationalCost` a los records.
- `infrastructure/in/web/dto/CatalogItemRequest.java` / `CatalogItemPatchRequest.java`
  / `CatalogItemResponse.java` — agregar los dos campos.
- `infrastructure/in/web/CatalogItemController.java` — pasar los dos campos nuevos al
  construir `CreateCatalogItemCommand`/`UpdateCatalogItemCommand` en `create()`/`update()`.
- `infrastructure/out/persistence/CatalogItemEntity.java` — agregar columnas `route`,
  `operational_cost`; actualizar constructor y getters.
- `infrastructure/out/persistence/CatalogItemRepositoryAdapter.java` — pasar los dos
  campos nuevos en `save()` (construcción de `CatalogItemEntity`) y en `toDomain()`
  (llamada a `reconstitute()`).
- `src/main/resources/db/migration/V13__add_transport_fields_to_catalog_items.sql` —
  `ALTER TABLE catalog_items ADD COLUMN route VARCHAR(200), ADD COLUMN
  operational_cost NUMERIC(12,2)`.

No se toca `CreateCatalogItemService`, `UpdateCatalogItemService`,
`DeactivateCatalogItemService`, `ReactivateCatalogItemService`,
`CatalogItemQueryService`, `CatalogItemRepositoryPort`, `CatalogItemJpaRepository`:
ninguno referencia campos por nombre uno a uno de forma que el cambio los rompa,
salvo el paso de parámetros ya cubierto arriba.

## Decisiones técnicas

- **Nombre del campo:** `operationalCost` (Java) / `operational_cost` (columna) en
  vez de `cost`. Alternativa descartada: `cost`, igual al Frontend. Motivo: evita
  confundirlo con el "costo operacional" general que spec 005 dejó fuera para
  HU-COST-001 — el nombre explícito dice que es específico de transporte.
- **Migración:** columnas nullable en `catalog_items` (`V13`). Alternativa
  descartada: tabla de extensión `transport_details`. Motivo: mismo patrón ya usado
  para `capacity` (opcional salvo `LODGING`); una tabla aparte para dos columnas es
  complejidad sin beneficio, contradice la regla 10 de CLAUDE.md ("preferir lo
  simple").
- **Sin validación de tipo cruzada:** no se agrega una regla que impida poner
  `route`/`operationalCost` en un `TOUR` o `FOOD`. Alternativa descartada: validar
  que solo apliquen a `TRANSPORT`. Motivo: ni el Frontend ni el PDR piden ese
  bloqueo; agregar la regla ahora es inventar un requisito (regla 7 de CLAUDE.md).

Ninguna de estas decisiones es candidata a ADR: son detalles de implementación de
una extensión de campo, no una decisión de arquitectura.

## Modelo de datos

`V13__add_transport_fields_to_catalog_items.sql`:

```sql
ALTER TABLE catalog_items
    ADD COLUMN route             VARCHAR(200),
    ADD COLUMN operational_cost  NUMERIC(12,2);
```

Nullable, sin `DEFAULT`: las filas existentes de `TOUR`/`LODGING`/`FOOD` quedan con
ambos campos en `NULL`, comportamiento correcto (no aplican).

## Contratos

`POST /api/tenants/{tenantId}/catalog-items`, `PATCH .../{itemId}`, `GET`
(list/detail) — mismos endpoints y códigos de spec 005. Único cambio: dos campos
opcionales nuevos en el body/response.

**Request (POST/PATCH), campos nuevos:**
```json
{
  "type": "TRANSPORT",
  "name": "Ruta Neiva - San Agustín",
  "price": 45000,
  "route": "Neiva - San Agustín",
  "operationalCost": 30000
}
```

**Response, campos nuevos:**
```json
{
  "route": "Neiva - San Agustín",
  "operationalCost": 30000
}
```

Sin cambios en códigos de error: `400 validation_error`, `404 not_found`,
`409 tenant_inactive` — los mismos handlers ya cubren `type: TRANSPORT`.

## Cómo se verifica

- Crear `TRANSPORT` sin `capacity`/`route`/`operationalCost` → `201`, los tres en
  `null` en la respuesta.
- Crear `TRANSPORT` con `route`/`operationalCost` → `GET /{itemId}` devuelve los
  mismos valores.
- `PATCH` solo `route` → `GET` posterior confirma que `operationalCost` y el resto
  de campos no cambiaron.
- Crear `LODGING` sin `capacity` → sigue devolviendo `400` (regresión de RN-HOS-003).
- Reutilizar los casos ya de spec 005 (aislamiento, soft delete/reactivate, tenant
  404/409) contra un ítem `TRANSPORT`.
- `./mvnw test` en verde.
