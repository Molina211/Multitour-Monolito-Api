# 006 — Consulta de reservas (listado y detalle) + alineación de `tenantId`

**Estado:** TERMINADA
**Fecha:** 2026-09-03
**Repos afectados:** backend
**HU relacionada:** HU-RES-008 (parcial, solo escenario 1 — dashboard diario), Source
RF-011. No cubre RN-EJE-005/RN-CAJ-001 (requieren ejecución y caja, fuera de alcance).

## Problema

`POST /api/reservations` (spec 001) crea reservas pero no existe ninguna forma de
consultarlas: no hay `GET` de listado ni de detalle. El Frontend
(`operator-reservation.service.ts`) simula por completo un listado y un detalle de
reservas en `localStorage`/`sessionStorage` (6 reservas quemadas) porque no hay backend
real detrás. Además, `reservations` resuelve `tenantId` como `UUID` vía un header
`X-Tenant-Id` (documentado en el propio código como "mecanismo de confianza temporal,
spec 001, decisión 1"), mientras que `tenants`/`catalog` (specs 002-005) lo resuelven
como `String` (slug) en la URL. Son dos mecanismos distintos para el mismo discriminador
de tenant dentro del mismo monolito, y cualquier consulta nueva sobre `reservations`
heredaría esa inconsistencia si no se corrige ahora.

## Alcance

- Alinear `reservations` al patrón ya establecido en `tenants`/`catalog`: `tenantId`
  pasa de `UUID` (header `X-Tenant-Id`) a `String` (slug, en la URL), igual que
  `Tenant.tenantId()`. Esto incluye:
  - Migración `V5__alter_reservations_tenant_id.sql` que cambia el tipo de columna
    `tenant_id` en `reservations` y `reserved_services` de `UUID` a `VARCHAR(50)`, y
    agrega `REFERENCES tenants(tenant_id)` (mismo patrón FK que `memberships` y
    `catalog_items`).
  - `Reservation.tenantId()` pasa de `UUID` a `String`.
  - `POST /api/reservations` se reubica a `POST /api/tenants/{tenantId}/reservations`
    (mismo patrón de tenant-en-URL que specs 003/004/005), eliminando el header
    `X-Tenant-Id` y el parseo de `UUID.fromString(...)`.
  - Igual que `CreateCatalogItemService`/`RegisterCustomerService`: rechaza
    `tenantId` inexistente (`404`) o `Inactivo` (`409`) antes de crear la reserva —
    hoy `POST /api/reservations` no hace ningún chequeo contra `tenants` en absoluto.
- Nuevo `GET /api/tenants/{tenantId}/reservations` — lista las reservas del tenant
  (HU-RES-008, escenario 1: dashboard diario), usando únicamente datos que
  `Reservation` ya tiene hoy: `reservationId`, `customerId`, `reservedServices`,
  `projectedValue`/`finalValue`/`pendingBalance`/`creditBalance`, `reservationStatus`,
  `paymentStatus`, `paymentMethod`, `createdAt`.
- Nuevo `GET /api/tenants/{tenantId}/reservations/{reservationId}` — detalle de una
  reserva puntual, mismos campos que el listado. `404` si no existe o pertenece a otro
  tenant (mismo criterio de aislamiento usado en specs 003-005).
- `ReservationRepositoryPort` gana `findByTenantIdAndReservationId` y
  `findAllByTenantId` (mismo patrón que `CatalogItemRepositoryPort`).
- `Reservation.reconstitute(...)` (no existe hoy — solo hay `create()`), siguiendo el
  mismo patrón que `Tenant`/`Membership`/`CatalogItem`, necesario para reconstruir el
  aggregate desde la fila persistida sin re-validar invariantes de creación.

## Fuera de alcance

- Pagos y su ciclo de estados (RN-RES-006A/006B/006C/006D, HU-CASH-*): el listado
  expone `paymentStatus`/`paymentMethod` tal como ya existen hoy en el aggregate, pero
  no se agrega ninguna transición nueva ni endpoint de registro/validación de pago.
- Descuentos (RN-RES-002, HU-DESC-001): no existe hoy ningún campo de descuento en
  `Reservation`; no se inventa uno para esta spec.
- Modificación de una reserva antes de ejecución (HU-RES-002, RN-RES-004) y
  reagendamiento (HU-RES-009, RN-EJE-006): son historias propias, requieren recalcular
  valores/disponibilidad — no se tocan aquí, esta spec es de solo lectura.
- Diferencia entre lo reservado y lo ejecutado (HU-RES-008, escenario 2): depende de
  HU-EXEC-001 (registro de ejecución), que no está implementada. Solo se cubre el
  escenario 1 de HU-RES-008 (listado/consulta), no el escenario 2.
- Devoluciones y caja (RN-RES-008, RN-RES-009, RN-CAJ-001, HU-RES008-CASH): fuera de
  alcance, dependen de un módulo de caja no implementado.
- Cancelación automática por vencimiento de plazo (RN-RES-006): requiere un mecanismo
  de tiempo/scheduler que no existe; no se agrega aquí.
- Reemplazar `OperatorReservationService`/`localStorage` en el Frontend por llamadas
  HTTP reales: igual que en specs 003/004/005, esta spec construye el backend; conectar
  el Frontend real queda para quien integre ambos repos.

## Criterios de aceptación

- [x] `POST /api/tenants/{tenantId}/reservations` con datos válidos de un tenant
      `Activo` devuelve `201` con la reserva creada (mismo comportamiento funcional
      que hoy, pero con `tenantId` en la URL en vez del header `X-Tenant-Id`).
- [x] `POST /api/tenants/{tenantId}/reservations` sobre un `tenantId` inexistente
      devuelve `404`; sobre uno `Inactivo` devuelve `409`.
- [x] El header `X-Tenant-Id` ya no existe en el contrato; `POST /api/reservations`
      (ruta vieja) deja de existir.
- [x] `GET /api/tenants/{tenantId}/reservations` devuelve todas las reservas del
      tenant (cualquier `reservationStatus`/`paymentStatus`), nunca reservas de otro
      tenant.
- [x] `GET /api/tenants/{tenantId}/reservations/{reservationId}` de una reserva de
      otro tenant devuelve `404` (aislamiento, no filtra existencia cruzada).
- [x] `GET /api/tenants/{tenantId}/reservations/{reservationId}` de una reserva
      existente devuelve todos sus `reservedServices` con `serviceReference`,
      `partySize` y `scheduledDate`.
- [x] La migración `V5` conserva las reservas creadas por la suite de verificación de
      spec 001 (o, si no hay datos reales que conservar en el entorno local, se deja
      explícito que la tabla se recrea vacía — ver "Riesgos y decisiones abiertas").
- [x] El proyecto compila y los tests existentes (specs 001-005) siguen pasando.

## Impacto en multitenencia

Este es el cambio central de la spec: `reservations` pasa de un aislamiento basado en
un header no verificado (`X-Tenant-Id`, cualquier `UUID` sintácticamente válido era
aceptado, sin comprobar que el tenant existiera) a la misma convención que
`tenants`/`catalog`: `tenantId` como slug en la URL, con existencia y estado (`Activo`/
`Inactivo`) verificados contra `TenantRepositoryPort` antes de cualquier escritura, y
todo `GET` filtrando siempre por `tenantId` además del identificador del recurso.

## Riesgos y decisiones abiertas

1. **Migración de tipo de columna, no solo aditiva**: `V5` altera `tenant_id` en
   `reservations`/`reserved_services` de `UUID` a `VARCHAR(50)`. Como no hay ningún
   `UUID` de tenant real persistido hoy que corresponda a un slug existente (el header
   `X-Tenant-Id` nunca se validó contra `tenants`), no hay manera de mapear filas viejas
   a un slug real — se decide truncar (`DROP`/`CREATE` de ambas tablas) en vez de
   intentar un `UPDATE` de conversión imposible. Se documenta como pérdida de datos de
   prueba, no de datos de negocio reales (el proyecto no está en producción).
2. **`Reservation.tenantId()` cambia de tipo (`UUID` → `String`)**: es un cambio que
   rompe el contrato de `CreateReservationCommand`/`ReservationResponse` tal como
   existen hoy. No hay consumidores reales del Frontend todavía (simula todo en
   `localStorage`), así que no hay integración externa que romper.
3. **No se agrega `reconstitute` con re-validación de invariantes de negocio nuevas**:
   `Reservation.reconstitute(...)` reconstruye tal cual está persistido, igual que
   `CatalogItem.reconstitute(...)`; no se revalida `RN-RES-001`/`RN-RES-003` al leer.

## Evidencia para la materia

Cierra parcialmente HU-RES-008 (escenario 1, RF-011) y corrige una inconsistencia
arquitectónica real entre bounded contexts del mismo monolito (`reservations` vs.
`tenants`/`catalog`), documentable como decisión de diseño en la sustentación.
