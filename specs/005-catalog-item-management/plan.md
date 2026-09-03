# 005 — Plan técnico

## Enfoque

Nuevo módulo `catalog` (bounded context "Operational Catalog"), con la misma
arquitectura hexagonal que `tenants`/`reservations`: un único aggregate `CatalogItem`
con un campo `type` (enum `TOUR`/`LODGING`/`FOOD`) en vez de tres entidades separadas
— mismo criterio ya usado en `Membership` (una sola clase, roles distintos vía enum,
con validación de invariantes condicionada al tipo). El módulo expone un CRUD-lite
sobre `/api/tenants/{tenantId}/catalog-items`, reutilizando el patrón ya validado en
`tenants` (`TenantRepositoryPort.findById(...).orElseThrow(TenantNotFoundException)` +
chequeo de `TenantStatus.INACTIVO` antes de cualquier escritura).

## Cambios por repositorio

Solo backend. Ningún cambio en Frontend ni Docs.

- `catalog/domain/model/CatalogItem.java` — aggregate root.
- `catalog/domain/model/CatalogItemType.java` — enum `TOUR`, `LODGING`, `FOOD`.
- `catalog/domain/exception/CatalogItemNotFoundException.java`
- `catalog/domain/exception/InvalidCatalogItemException.java` (capacidad faltante o
  inválida en `LODGING`, nombre/precio requeridos, etc.)
- `catalog/domain/port/in/{CreateCatalogItemCommand, CreateCatalogItemUseCase,
  UpdateCatalogItemCommand, UpdateCatalogItemUseCase, DeactivateCatalogItemUseCase,
  ReactivateCatalogItemUseCase, CatalogItemQueryUseCase}.java`
- `catalog/domain/port/out/CatalogItemRepositoryPort.java`
- `catalog/application/{CreateCatalogItemService, UpdateCatalogItemService,
  DeactivateCatalogItemService, ReactivateCatalogItemService,
  CatalogItemQueryService}.java`
- `catalog/infrastructure/in/web/CatalogItemController.java` +
  `dto/{CatalogItemRequest, CatalogItemPatchRequest, CatalogItemResponse}.java`
- `catalog/infrastructure/out/persistence/{CatalogItemEntity,
  CatalogItemJpaRepository, CatalogItemRepositoryAdapter}.java`
- Reutiliza `tenants/domain/port/out/TenantRepositoryPort` (ya existe, sin cambios) y
  `tenants/domain/exception/{TenantNotFoundException, TenantInactiveException}` (ya
  existen — un módulo de negocio puede depender de los puertos/excepciones de dominio
  de otro dentro del mismo monolito modular, igual que ya ocurre entre `tenants` y
  `common/audit`).
- `src/main/resources/db/migration/V4__create_catalog_items.sql`

## Decisiones técnicas

- **Una sola entidad `CatalogItem` con discriminador `type`, no tres entidades**:
  alternativa descartada — `Attraction`/`Lodging`/`FoodOption` separadas. Motivo: los
  tres tipos comparten el 90% de los campos (nombre, precio, vigencia, política,
  imagen, estado) y el Frontend ya los trata de forma casi idéntica (mismos
  componentes `manage-*` con la misma estructura); solo `capacity` es
  condicionalmente obligatorio (`LODGING`). Tres entidades casi iguales sería
  duplicación sin beneficio real, igual que ya se decidió con `Membership`.
- **`capacity` es `Integer` nullable a nivel de columna, pero obligatorio y positivo en
  el dominio solo cuando `type = LODGING`** (RN-HOS-003): alternativa descartada —
  columna `NOT NULL` con `0` como valor por defecto para tours/comida, descartada
  porque `0` no es "sin capacidad", es un valor de negocio ambiguo (¿un tour con
  capacidad cero?); mejor `NULL` explícito para "no aplica a este tipo".
  Es una validación que ya se venía haciendo en `Membership.createAdministrator` vs
  `createEndCustomer` (invariantes condicionadas al tipo/rol dentro del mismo factory).
- **Sin costos operacionales (segunda mitad de RN-ATR-001)**: no hay campo para eso en
  `CatalogItem`. Ya documentado como fuera de alcance en `spec.md` — pertenece a una
  historia propia (HU-COST-001) no implementada, y el formulario real del Frontend
  (`new-service.component.ts`) tampoco lo captura.
- **Reactivación explícita, nunca implícita**: mismo criterio que `Tenant.reactivate()`
  (INV-TEN-002) — desactivar preserva la fila y su historial; solo un endpoint
  explícito de reactivación la vuelve a poner activa.
- **`image` como `String` (URL/referencia), sin tabla ni bucket de archivos**: decisión
  ya fijada en `spec.md`, sección "Riesgos y decisiones abiertas".
- **Sin resolución de rol/actor para HU-CAT-001 escenario 2**: ningún endpoint de este
  módulo valida "quién" hace la petición más allá de lo que ya no se valida en
  `tenants` (el mismo vacío de `permitAll()` heredado desde spec 001). Documentado como
  fuera de alcance en `spec.md`, no se resuelve aquí.

## Modelo de datos

Nueva migración `V4__create_catalog_items.sql`:

```sql
CREATE TABLE catalog_items (
    catalog_item_id   UUID PRIMARY KEY,
    tenant_id         VARCHAR(50) NOT NULL REFERENCES tenants(tenant_id),
    item_type         VARCHAR(20) NOT NULL,
    name              VARCHAR(150) NOT NULL,
    price             NUMERIC(12,2) NOT NULL,
    capacity          INTEGER,
    restrictions      VARCHAR(500),
    valid_from        DATE,
    valid_to          DATE,
    policy            VARCHAR(500),
    image             VARCHAR(500),
    active            BOOLEAN NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_catalog_items_tenant ON catalog_items(tenant_id);
```

`price` usa `NUMERIC(12,2)` mapeado a `BigDecimal`, mismo patrón que las columnas
monetarias de `reservations` (`V1__create_reservations.sql`).

## Contratos

Todos bajo `/api/tenants/{tenantId}/catalog-items`, mismo patrón de tenant-en-URL que
specs 003/004.

- `POST` — body: `{name, type, price, capacity?, restrictions?, validFrom?, validTo?,
  policy?, image?}`. `201` con el ítem creado (`active: true`). `400` si `type=LODGING`
  y falta/`capacity<=0`, o si `name`/`price`/`type` faltan. `404` si el tenant no
  existe. `409` si el tenant está `Inactivo`.
- `GET` — `200` con la lista completa del tenant (activos e inactivos). `404` si el
  tenant no existe.
- `GET /{itemId}` — `200` con el ítem. `404` si no existe o pertenece a otro tenant
  (mismo criterio de no distinguir "no existe" de "es de otro tenant", ya usado para
  aislamiento en otros módulos).
- `PATCH /{itemId}` — body con los campos a cambiar (todos opcionales, se conservan los
  no enviados). `200` con el ítem actualizado. Mismas validaciones de `POST` sobre el
  resultado final (si el `PATCH` deja un `LODGING` sin capacidad válida, se rechaza).
- `POST /{itemId}/deactivate` — sin body. `200` con `active: false`. `409` si ya estaba
  inactivo (mismo criterio que `Tenant.deactivate()`).
- `POST /{itemId}/reactivate` — sin body. `200` con `active: true`. `409` si ya estaba
  activo.

## Cómo se verifica

- Cada criterio de aceptación de `spec.md` con un `curl` correspondiente en la nueva
  sección "005" de `PLAN-VERIFICACION.md`.
- `./mvnw test` en verde (specs 001-004 sin regresión).
