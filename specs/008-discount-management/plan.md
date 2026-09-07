# 008 — Plan técnico

## Enfoque

Nuevo bounded context `discounts`, calcado del patrón hexagonal ya usado en `catalog`
(spec 005): un agregado inmutable con `create`/`reconstitute`/`update`/`deactivate`/
`reactivate`, casos de uso separados por operación, adapter JPA con Flyway, y un
controller con el mismo estilo de `@ExceptionHandler` que ya usan `CatalogItemController`
y `ReservationController`. La única validación cruzada es que `catalogItemId` exista y
pertenezca al mismo tenant — se resuelve reutilizando directamente
`CatalogItemRepositoryPort` (ya expone `findByTenantIdAndCatalogItemId`), sin crear un
segundo servicio de consulta.

## Cambios por repositorio

Solo backend, en la rama `hu-back-001-dev`.

- `discounts/domain/model/Discount.java` — agregado: `discountId` (UUID), `tenantId`,
  `catalogItemId` (UUID), `percentage` (int 1-100), `validFrom`/`validTo` (LocalDate),
  `priority` (int), `stackable` (boolean), `cap` (BigDecimal, nullable), `base`
  (`DiscountBase`), `active`, `createdAt`. `create(...)` valida: `percentage` 1-100,
  `validFrom <= validTo` (si ambos vienen), `cap` positivo si viene. **No valida solapes
  de vigencia con otros descuentos** (spec 008, decisión explícita — RF-005A/RF-005B).
- `discounts/domain/model/DiscountBase.java` — enum `ORIGINAL_VALUE`,
  `PREVIOUS_SUBTOTAL` (mismos valores que el Frontend envía como `"original"`/
  `"subtotal"`, mapeados en el DTO, no en el dominio).
- `discounts/domain/exception/InvalidDiscountException.java` — mismo rol que
  `InvalidCatalogItemException`.
- `discounts/domain/exception/DiscountNotFoundException.java` — mismo rol que
  `CatalogItemNotFoundException`.
- `discounts/domain/port/in/{CreateDiscountCommand, CreateDiscountUseCase,
  UpdateDiscountCommand, UpdateDiscountUseCase, DeactivateDiscountUseCase,
  ReactivateDiscountUseCase, DiscountQueryUseCase}.java` — un puerto por operación,
  mismo criterio que `catalog`.
- `discounts/domain/port/out/DiscountRepositoryPort.java` — `save`,
  `findByTenantIdAndDiscountId`, `findAllByTenantId`.
- `discounts/application/{CreateDiscountService, UpdateDiscountService,
  DeactivateDiscountService, ReactivateDiscountService, DiscountQueryService}.java` —
  `CreateDiscountService` inyecta también `TenantRepositoryPort` (valida tenant
  existe/activo, igual que `CreateCatalogItemService`) y `CatalogItemRepositoryPort`
  (valida `catalogItemId` existe en ese tenant, lanza `CatalogItemNotFoundException` si
  no — se reutiliza la excepción de `catalog`, no se crea una nueva).
- `discounts/infrastructure/out/persistence/{DiscountEntity, DiscountJpaRepository,
  DiscountRepositoryAdapter}.java` — mismo patrón que `catalog`.
- `discounts/infrastructure/in/web/DiscountController.java` — rutas bajo
  `/api/tenants/{tenantId}/discounts`; `@ExceptionHandler` para
  `InvalidDiscountException`/`IllegalArgumentException` (`400`),
  `TenantNotFoundException`/`CatalogItemNotFoundException`/`DiscountNotFoundException`
  (`404`), `TenantInactiveException` (`409`) — reutiliza las excepciones de `tenants` y
  `catalog` tal como ya hace `CatalogItemController`.
- `discounts/infrastructure/in/web/dto/{DiscountRequest, DiscountPatchRequest,
  DiscountResponse}.java`.
- `db/migration/V6__create_discounts.sql` — nueva tabla `discounts`, FK a `tenants` y a
  `catalog_items`.
- `SecurityConfig.java` — **sin cambios**: `anyRequest().permitAll()` ya cubre estas
  rutas nuevas (solo `POST /reservations` está protegido desde spec 007).

## Decisiones técnicas

- **Reutilizar `CatalogItemRepositoryPort` en vez de crear un `DiscountCatalogQuery`
  propio**: ya expone exactamente `findByTenantIdAndCatalogItemId`, que es lo único que
  se necesita para validar la referencia cruzada — evita duplicar una consulta que ya
  existe.
- **Reutilizar `CatalogItemNotFoundException`/`TenantNotFoundException`/
  `TenantInactiveException` en `DiscountController` en vez de crear equivalentes
  propios**: mismo criterio ya aplicado por `CatalogItemController` (que reutiliza las
  excepciones de `tenants`) — son la misma condición de error, no hace falta una clase
  nueva por módulo.
- **No hay validación de solape de vigencias**: decisión explícita de la spec (RF-005A
  permite descuentos simultáneos; RF-005B deja su combinación a la aplicación real, fuera
  de alcance aquí). `Discount.create` no consulta otros descuentos existentes.
- **`base` se modela como enum en el dominio pero se mapea desde/hacia
  `"original"`/`"subtotal"` en el DTO**: son los valores literales que ya envía el
  Frontend (`new-discount.component.ts`); mapear en el borde (DTO) y no en el dominio
  evita acoplar el nombre del enum Java a un string del Frontend que puede cambiar.
- **`SecurityConfig` no cambia**: `anyRequest().permitAll()` ya es la regla por defecto;
  no hace falta agregar una entrada explícita para rutas que ya caen en el default.

## Modelo de datos

Migración `V6__create_discounts.sql`:

```sql
CREATE TABLE discounts (
    discount_id       UUID PRIMARY KEY,
    tenant_id         VARCHAR(50) NOT NULL REFERENCES tenants(tenant_id),
    catalog_item_id   UUID NOT NULL REFERENCES catalog_items(catalog_item_id),
    percentage        INTEGER NOT NULL,
    valid_from        DATE,
    valid_to          DATE,
    priority          INTEGER NOT NULL DEFAULT 0,
    stackable         BOOLEAN NOT NULL DEFAULT FALSE,
    cap               NUMERIC(12,2),
    base              VARCHAR(20) NOT NULL,
    active            BOOLEAN NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_discounts_tenant ON discounts(tenant_id);
CREATE INDEX idx_discounts_catalog_item ON discounts(catalog_item_id);
```

Sin índice único de vigencia — se permiten filas solapadas a propósito (ver Decisiones
técnicas).

## Contratos

- `POST /api/tenants/{tenantId}/discounts` — body:
  `{catalogItemId, percentage, validFrom, validTo, priority, stackable, cap, base}`.
  `201` con el descuento creado · `400` si `percentage` fuera de 1-100 o
  `validFrom > validTo` · `404` si el tenant o el `catalogItemId` no existen (o el
  `catalogItemId` es de otro tenant) · `409` si el tenant está `Inactivo`.
- `GET /api/tenants/{tenantId}/discounts` — `200`, lista filtrada por tenant.
- `GET /api/tenants/{tenantId}/discounts/{discountId}` — `200` / `404`.
- `PATCH /api/tenants/{tenantId}/discounts/{discountId}` — mismo patrón PATCH que
  `CatalogItemController` (campos `null` no se tocan). `200` / `400` / `404`.
- `POST .../{discountId}/deactivate` y `.../{discountId}/reactivate` — `200` / `404` /
  `400` si ya está en ese estado (mismo criterio que `CatalogItem.deactivate/reactivate`).

## Cómo se verifica

- Cada criterio de aceptación de `spec.md` con un `curl` en la nueva sección "008" de
  `PLAN-VERIFICACION.md`, incluyendo explícitamente el caso de dos descuentos activos con
  vigencia solapada sobre el mismo `catalogItemId` (debe devolver `201` las dos veces).
- `./mvnw test` en verde (specs 001-007 sin regresión).
