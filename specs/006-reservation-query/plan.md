# 006 — Plan técnico

## Enfoque

Dos cambios entrelazados sobre el módulo `reservations` existente (spec 001), sin
crear un módulo nuevo: (1) alinear `tenantId` de `UUID`/header a `String`/URL, mismo
patrón que `tenants`/`catalog`, reutilizando `TenantRepositoryPort.findById(...)` +
chequeo de `TenantStatus.INACTIVO` (igual que `CreateCatalogItemService`); (2) agregar
lectura (`findAllByTenantId`, `findByTenantIdAndReservationId`) al puerto de salida y
dos `GET` nuevos al controller existente, con un `ReservationQueryService` nuevo (mismo
rol que `CatalogItemQueryService`). No se toca la lógica de negocio de `Reservation`
más allá de cambiar el tipo de `tenantId` y agregar `reconstitute(...)`.

## Cambios por repositorio

Solo backend. Ningún cambio en Frontend ni Docs.

- `reservations/domain/model/Reservation.java` — `tenantId` pasa de `UUID` a `String`;
  se agrega `reconstitute(...)` (sin re-validar invariantes), mismo patrón que
  `CatalogItem.reconstitute(...)`.
- `reservations/domain/port/in/CreateReservationCommand.java` — `tenantId` de `UUID` a
  `String`.
- `reservations/domain/port/in/{ReservationQueryUseCase}.java` — nuevo, `getById(String
  tenantId, UUID reservationId)` y `listByTenant(String tenantId)`.
- `reservations/domain/port/out/ReservationRepositoryPort.java` — agrega
  `findByTenantIdAndReservationId(String tenantId, UUID reservationId)` y
  `findAllByTenantId(String tenantId)`.
- `reservations/application/CreateReservationService.java` — resuelve tenant vía
  `TenantRepositoryPort.findById(...).orElseThrow(TenantNotFoundException::new)`,
  rechaza `TenantStatus.INACTIVO` con `TenantInactiveException`, igual que
  `CreateCatalogItemService`. Requiere inyectar `TenantRepositoryPort` (de `tenants`,
  mismo cruce de módulos ya usado en `catalog`).
- `reservations/application/ReservationQueryService.java` — nuevo, mismo patrón que
  `CatalogItemQueryService` (`tenantRepositoryPort.existsById(...)` para chequeo
  liviano de lectura, sin exigir `Activo`).
- `reservations/infrastructure/in/web/ReservationController.java` —
  `@RequestMapping("/api/tenants/{tenantId}/reservations")`, elimina
  `@RequestHeader X-Tenant-Id` y `parseTenantId(...)`; agrega `GET` (listado) y `GET
  /{reservationId}` (detalle); agrega manejadores `TenantNotFoundException/
  ReservationNotFoundException → 404` y `TenantInactiveException → 409` (mismos
  códigos que `CatalogItemController`).
- `reservations/infrastructure/in/web/dto/CreateReservationRequest.java` — sin
  cambios (ya no llevaba `tenantId`, lo tomaba del header; ahora lo toma de la URL,
  igual que `CatalogItemRequest`).
- `reservations/infrastructure/in/web/dto/ReservationResponse.java` — `tenantId` de
  `UUID` a `String`; se agrega un DTO `ReservedServiceResponse` (hoy no existe
  representación de salida de los servicios reservados, solo de entrada) para exponer
  `serviceReference`/`partySize`/`scheduledDate` en el detalle.
- `reservations/domain/exception/ReservationNotFoundException.java` — nuevo, mismo rol
  que `CatalogItemNotFoundException`.
- `reservations/infrastructure/out/persistence/ReservationEntity.java` y
  `ReservedServiceEntity.java` — `tenantId` de `UUID` a `String`.
- `reservations/infrastructure/out/persistence/ReservationJpaRepository.java` — agrega
  `findByTenantIdAndReservationId` y `findAllByTenantId` (Spring Data derived
  queries), mismo patrón que `CatalogItemJpaRepository`.
- `reservations/infrastructure/out/persistence/ReservationRepositoryAdapter.java` —
  implementa los dos métodos nuevos del puerto, mapeando entidad↔dominio vía
  `Reservation.reconstitute(...)` y `ReservedService` (record, sin cambios).
- Reutiliza `tenants/domain/port/out/TenantRepositoryPort` y
  `tenants/domain/exception/{TenantNotFoundException, TenantInactiveException}` (ya
  existen, sin cambios) — mismo cruce de módulos ya justificado en spec 005.
- `src/main/resources/db/migration/V5__alter_reservations_tenant_id.sql`

## Decisiones técnicas

- **Migración destructiva de `tenant_id` (`DROP`/`CREATE` en vez de `ALTER COLUMN ...
  USING`)**: alternativa descartada — convertir cada `UUID` existente a un slug real
  vía `UPDATE`, descartada porque no existe ninguna correspondencia real entre los
  `UUID` que pudo haber aceptado el header `X-Tenant-Id` (nunca se validaron contra
  `tenants`) y los slugs reales (`travesia-natural`, etc.). Son datos de prueba de un
  proyecto que no está en producción (ya documentado como riesgo 1 en `spec.md`).
- **`tenantId` como `String` en `Reservation`, no un value object propio**: mismo
  criterio que `Tenant.tenantId()` — un slug validado como no-nulo/no-blank en el
  dominio, sin envolver en una clase extra, consistente con el resto del código.
- **Chequeo de tenant en creación (`findById` + `INACTIVO`) vs. en lectura
  (`existsById`)**: mismo criterio ya usado en `catalog` — escribir requiere saber si
  el tenant está activo, leer no.
- **`ReservationQueryService` como clase nueva, no fusionar con
  `CreateReservationService`**: mismo criterio de separación ya usado entre
  `CreateCatalogItemService` y `CatalogItemQueryService` — comandos y consultas en
  clases distintas dentro del mismo módulo.
- **Sin `UpdateReservationUseCase` ni endpoints de modificación**: HU-RES-002/HU-RES-009
  quedan fuera de alcance (ya documentado en `spec.md`); esta spec es de solo lectura
  más el alta ya existente.
- **`ReservedServiceResponse` nuevo en vez de reusar `ReservedServiceRequest`**: el
  request no tiene sentido como forma de salida (no HTTP-símetro server no debería
  aceptar lo que devuelve como si fuera lo mismo); se separa igual que ya está separado
  en otras specs (`CatalogItemRequest` vs. `CatalogItemResponse`).
- **`@Transactional(readOnly = true)` en `findByTenantIdAndReservationId`/
  `findAllByTenantId` de `ReservationRepositoryAdapter`**: bug real encontrado durante
  la verificación T10 (`LazyInitializationException` al mapear
  `ReservationEntity.reservedServices`, que es `@OneToMany` perezoso por defecto, fuera
  de una sesión de Hibernate abierta). Es el primer uso de `@Transactional` en el
  proyecto — no hace falta en `CatalogItemRepositoryAdapter`/`TenantRepositoryAdapter`
  porque ninguno de esos aggregates tiene una colección `@OneToMany` que mapear al
  leer.

## Modelo de datos

Nueva migración `V5__alter_reservations_tenant_id.sql`:

```sql
DROP TABLE IF EXISTS reserved_services;
DROP TABLE IF EXISTS reservations;

CREATE TABLE reservations (
    reservation_id     UUID PRIMARY KEY,
    tenant_id          VARCHAR(50) NOT NULL REFERENCES tenants(tenant_id),
    customer_id        VARCHAR(100) NOT NULL,
    projected_value    NUMERIC(12,2) NOT NULL,
    final_value        NUMERIC(12,2) NOT NULL,
    pending_balance    NUMERIC(12,2) NOT NULL,
    credit_balance     NUMERIC(12,2) NOT NULL DEFAULT 0,
    reservation_status VARCHAR(30) NOT NULL,
    payment_status     VARCHAR(30) NOT NULL,
    payment_method     VARCHAR(30),
    created_at         TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE reserved_services (
    id                 BIGSERIAL PRIMARY KEY,
    reservation_id     UUID NOT NULL REFERENCES reservations(reservation_id),
    tenant_id          VARCHAR(50) NOT NULL REFERENCES tenants(tenant_id),
    service_reference  VARCHAR(100) NOT NULL,
    party_size         INTEGER,
    scheduled_date     DATE
);

CREATE INDEX idx_reservations_tenant ON reservations(tenant_id);
CREATE INDEX idx_reserved_services_tenant ON reserved_services(tenant_id);
CREATE INDEX idx_reserved_services_reservation ON reserved_services(reservation_id);
```

Recrea ambas tablas desde cero con `tenant_id VARCHAR(50)` y FK a `tenants(tenant_id)`
(mismo patrón que `memberships`/`catalog_items`), igual que ya definía `V1`, solo
cambiando el tipo de columna. No hay `ALTER COLUMN ... USING` posible sin datos reales
que mapear (ver "Decisiones técnicas").

## Contratos

- `POST /api/tenants/{tenantId}/reservations` — body: `{customerId, projectedValue,
  reservedServices: [{serviceReference, partySize?, scheduledDate?}]}`. `201` con la
  reserva creada. `400` si `customerId`/`projectedValue`/`reservedServices` faltan o
  son inválidos (igual que hoy). `404` si el tenant no existe. `409` si está
  `Inactivo`. Reemplaza a `POST /api/reservations` (ruta y header eliminados).
- `GET /api/tenants/{tenantId}/reservations` — `200` con la lista completa del tenant
  (cualquier estado). `404` si el tenant no existe.
- `GET /api/tenants/{tenantId}/reservations/{reservationId}` — `200` con la reserva,
  incluidos sus `reservedServices`. `404` si no existe o pertenece a otro tenant (mismo
  criterio de no distinguir "no existe" de "es de otro tenant" ya usado en `catalog`).

## Cómo se verifica

- Cada criterio de aceptación de `spec.md` con un `curl` correspondiente en la nueva
  sección "006" de `PLAN-VERIFICACION.md`.
- `./mvnw test` en verde (specs 001-005 sin regresión).
