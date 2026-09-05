# 025 — Plan técnico

## Enfoque

Módulo nuevo `establishments`, hexagonal, calcado del patrón de `catalog` (spec 005):
un agregado inmutable con métodos `create`/`reconstitute`/`deactivate`/`reactivate`,
puertos in/out, un servicio de aplicación por caso de uso, persistencia JPA con
adaptador, y un controller bajo `/api/tenants/{tenantId}/establishments` que reutiliza
las mismas excepciones de tenant (`TenantNotFoundException`, `TenantInactiveException`)
que ya usa `CatalogItemController`.

## Decisiones abiertas de la spec — resueltas

1. **Módulo propio, no extensión de `catalog`.** `CatalogItemType` no tiene un valor
   que le quede bien (un establecimiento no es un TOUR/LODGING/FOOD/TRANSPORT: no
   tiene precio, capacidad ni vigencia) y forzarlo dejaría columnas nulas sin sentido
   en `catalog_items`. Un agregado y tabla propios son más simples de leer y de
   sustentar.
2. **`image` como columna de texto simple** (`VARCHAR(500)`), igual que
   `catalog_items.image` — sin almacenamiento de binarios, mismo criterio ya usado.

## Cambios por repositorio

Todo en `backend`, paquete `com.corhuila.errorcapa8.travesia_natural.establishments`:

- `domain/model/AssociatedEstablishment.java` — agregado (mismo estilo que
  `CatalogItem.java`: constructor privado, `create`/`reconstitute`/`deactivate`/
  `reactivate`, getters).
- `domain/model/EstablishmentKind.java` — enum `HOTEL`, `RESTAURANT`.
- `domain/exception/InvalidEstablishmentException.java`,
  `EstablishmentNotFoundException.java`.
- `domain/port/in/CreateEstablishmentCommand.java`, `CreateEstablishmentUseCase.java`,
  `EstablishmentQueryUseCase.java`, `DeactivateEstablishmentUseCase.java`,
  `ReactivateEstablishmentUseCase.java`.
- `domain/port/out/EstablishmentRepositoryPort.java` — `save`,
  `findByTenantIdAndEstablishmentId`, `findAllByTenantId`.
- `application/CreateEstablishmentService.java` (valida tenant existente/activo, igual
  que `CreateCatalogItemService`), `EstablishmentQueryService.java`,
  `DeactivateEstablishmentService.java`, `ReactivateEstablishmentService.java`.
- `infrastructure/in/web/EstablishmentController.java` +
  `dto/EstablishmentRequest.java` + `dto/EstablishmentResponse.java`.
- `infrastructure/out/persistence/EstablishmentEntity.java`,
  `EstablishmentJpaRepository.java`, `EstablishmentRepositoryAdapter.java`.
- `src/main/resources/db/migration/V21__create_establishments.sql`.

No se toca ningún archivo existente de `catalog`, `tenants` ni `common` — solo se
reutilizan sus excepciones/puertos ya públicos.

## Decisiones técnicas

- **Reutilizar `TenantRepositoryPort`/`TenantNotFoundException`/`TenantInactiveException`
  del módulo `tenants`** en vez de duplicar la validación — mismo criterio que `catalog`
  y `cash`; alternativa (validar tenant dentro del propio módulo) descartada por
  duplicar lógica que ya existe.
- **Sin capa de `update` (PATCH)**: la spec no lo pide (el Frontend no edita, solo crea/
  lista/activa/desactiva) — alternativa de copiar el PATCH de `CatalogItem` descartada
  por no tener criterio de aceptación que lo respalde (regla de no construir de más).
- **`kind` como enum de dominio, serializado como `String` en la entidad** — mismo
  patrón que `CatalogItemType`/`itemType`.

## Modelo de datos

Tabla nueva `establishments` (migración `V21__create_establishments.sql`):

```sql
CREATE TABLE establishments (
    establishment_id  UUID PRIMARY KEY,
    tenant_id         VARCHAR(50) NOT NULL REFERENCES tenants(tenant_id),
    kind              VARCHAR(20) NOT NULL,
    name              VARCHAR(150) NOT NULL,
    description       VARCHAR(500),
    image             VARCHAR(500),
    active            BOOLEAN NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_establishments_tenant ON establishments(tenant_id);
```

## Contratos

Base: `/api/tenants/{tenantId}/establishments`

- `POST /` — body `{kind, name, description, image}` → `201` +
  `EstablishmentResponse` (`active: true`). `400` si `name` vacío/nulo o `kind` fuera de
  `HOTEL`/`RESTAURANT`. `404` tenant inexistente, `409` tenant inactivo.
- `GET /` — `200` + `List<EstablishmentResponse>` (activos e inactivos) del tenant.
- `POST /{establishmentId}/deactivate` — `200` + `EstablishmentResponse` con
  `active: false`. `404` si el establecimiento no existe.
- `POST /{establishmentId}/reactivate` — `200` + `EstablishmentResponse` con
  `active: true`.

`EstablishmentResponse`: `establishmentId, tenantId, kind, name, description, image,
active, createdAt`.

## Cómo se verifica

Todo por `curl` contra el servidor local (arranque bajo autorización explícita, regla 5
de CLAUDE.md):

1. `POST .../establishments` con datos válidos → `201`, `active: true`.
2. `POST .../establishments` sin `name` y con `kind` inválido → `400` en ambos casos.
3. `GET .../establishments` de dos tenants distintos → cada uno ve solo lo suyo.
4. `POST .../{id}/deactivate` sobre uno activo → `200`, `active: false`, sigue en el
   `GET`.
5. `POST .../{id}/reactivate` sobre ese mismo → `200`, `active: true`.
6. Cualquier operación con `tenantId` inexistente → `404`; con uno desactivado → `409`.
7. `mvn compile` (o build del IDE) sin errores, sin tocar nada de specs 001-024.
