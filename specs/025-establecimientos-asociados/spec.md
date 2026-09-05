# 025 — Establecimientos asociados (hoteles y restaurantes)

**Estado:** TERMINADA
**Fecha:** 2026-09-05
**Repos afectados:** backend
**HU relacionada:** ninguna HU formal en el backlog; se basa en RN-ASO-001 del PDR
(`03-product/prd.md`, confirmada).

## Problema

RN-ASO-001 permite a un tenant registrar y publicar establecimientos asociados a su
operación (hoteles y restaurantes) con fines de promoción comercial, como entidades
distintas de los `CatalogItem` operativos (`TOUR`/`LODGING`/`FOOD`/`TRANSPORT`, spec 005/
015): no llevan precio, capacidad, vigencia ni se reservan. El Frontend ya construyó el
flujo completo para esto — creación (`new-service.component.ts`, tipo "Establecimiento
asociado"), listado por tipo (`client-restaurants.component.ts`, `client-lodging
.component.ts`), listado combinado (`client-gastronomy.component.ts`) y administración
de estado activo/inactivo (`manage-restaurants.component.ts`) — pero todo vive hoy en
`localStorage` (`OperatorCatalogService`, clave `multitour-associated-establishments`),
sin ningún concepto equivalente en el dominio del Backend.

## Alcance

- Nuevo módulo `establishments` (o equivalente, se confirma en `/plan-tareas`), mismo
  patrón hexagonal que `catalog`/`operations`.
- Agregado con los mismos campos que ya usa el Frontend (`AssociatedEstablishment`,
  `operator-catalog.service.ts`): `tenantId`, `kind` (`HOTEL` | `RESTAURANT`), `name`
  (nombre comercial), `description` (información comercial básica, texto libre),
  `image` (URL/texto, mismo criterio ya usado por `CatalogItem.image` en spec 005),
  `active` (booleano, `true` por defecto al crear).
- Endpoints bajo `/api/tenants/{tenantId}/establishments` (mismo patrón de tenant-en-URL
  que `catalog-items`, spec 005):
  - `POST` — crea el establecimiento (`active: true` por defecto).
  - `GET` — lista los establecimientos del tenant (activos e inactivos, igual que
    `GET /catalog-items` en spec 005 — el filtro "solo activos" para las pantallas de
    Cliente ya lo aplica el Frontend, no se repite en el Backend salvo que una pantalla
    real lo necesite).
  - `POST /{establishmentId}/deactivate` — desactiva (soft, nunca se borra la fila —
    mismo criterio de RN-ASO-001: "conservando su información e histórico").
  - `POST /{establishmentId}/reactivate` — vuelve a activar.
- Rechaza operaciones sobre un tenant inexistente (`404`) o `Inactivo` (`409`), mismo
  criterio que el resto del proyecto.
- `name` obligatorio y no vacío (único dato que el formulario real exige junto a la
  imagen); `kind` restringido a los dos valores soportados por el Frontend.

## Fuera de alcance

- `GET /{establishmentId}` (detalle individual): ninguna pantalla real navega a un
  detalle de establecimiento — `client-restaurants`/`client-lodging` solo listan
  tarjetas con los mismos campos ya devueltos por el listado. No se crea un endpoint
  que ninguna pantalla pide (mismo criterio que specs previas).
- Habitaciones, platos, menús, cupos, tarifas de reserva o disponibilidad operativa del
  establecimiento: RN-ASO-001 los excluye explícitamente ("no implica registrar
  habitaciones, platos, menús, cupos, tarifas de reserva ni disponibilidad operativa").
- Restricción real de rol (`Administrador` puede crear/activar, `Colaborador` solo
  consulta) — el Frontend ya la aplica en su propia capa (`colaboradorRestrictedGuard`
  en la ruta de creación, `roleService.isColaborador()` en el toggle de
  `manage-restaurants.component.ts`), pero el Backend no valida rol real en ningún
  endpoint de catálogo hoy (mismo criterio ya fijado en spec 005 para `CatalogItem`, sin
  cambios desde entonces). Se deja para si una spec futura introduce roles reales de
  operador en el Backend.
- Vincular un establecimiento a un `CatalogItem` de tipo `FOOD` (plato del día con
  restaurante) o a una reserva: el Frontend ya resuelve ese enlace mostrando ambos lado
  a lado en la misma grilla (`client-gastronomy.component.ts`), sin una relación real
  entre ambos registros — RN-ASO-001 los trata como entidades comerciales
  independientes, no se inventa una relación que ninguna regla ni pantalla pide.
- Conectar el Frontend a este endpoint: `OperatorCatalogService` sigue en `localStorage`;
  integración diferida hasta que ambos módulos estén completos (mismo criterio que
  specs 013/022/023/024).

## Criterios de aceptación

- [x] Crear un establecimiento con `tenantId`, `kind` (`HOTEL` o `RESTAURANT`), `name` e
      `image` válidos devuelve `201`, `active: true` por defecto.
- [x] Crear un establecimiento sin `name`, o con `kind` fuera de `HOTEL`/`RESTAURANT`,
      devuelve `400`.
- [x] Consultar `GET /establishments` de un tenant devuelve todos sus establecimientos
      (activos e inactivos), sin mezclar con los de otro tenant.
- [x] Desactivar un establecimiento activo devuelve `200`, pasa a `active: false`, y
      sigue apareciendo en `GET /establishments` con su información e histórico
      intactos (nunca se borra la fila).
- [x] Reactivar un establecimiento inactivo devuelve `200` y vuelve a `active: true`.
- [x] Cualquier operación sobre un `tenantId` inexistente devuelve `404`; sobre uno
      `Inactivo`, `409`.
- [x] El proyecto compila y los tests existentes (specs 001-024) siguen pasando.

## Impacto en multitenencia

Todo establecimiento queda asociado a un `tenantId`; crear, listar, desactivar y
reactivar siempre filtran por el `tenantId` de la URL — mismo patrón de aislamiento ya
aplicado en `catalog` (spec 005) y `cash` (spec 013). RN-ASO-001 lo confirma
explícitamente: "los establecimientos asociados son visibles únicamente dentro del
tenant que mantiene la asociación".

## Riesgos y decisiones abiertas

1. **Nombre del módulo/agregado**: módulo propio `establishments` con su agregado
   `AssociatedEstablishment`, vs. extender el módulo `catalog` ya existente con un
   agregado adicional (sin usar `CatalogItemType`, que no le queda bien — no tiene
   precio ni vigencia). Se decide en `/plan-tareas`, no cambia ningún criterio de
   aceptación.
2. **Persistencia de `image`**: mismo criterio ya usado (o pendiente de decidir) por
   `CatalogItem.image` en spec 005 — texto/URL en columna simple, sin almacenamiento de
   binarios en esta fase. Se confirma en `/plan-tareas`.

## Evidencia para la materia

Cierra el único hueco de funcionalidad real detectado el 2026-09-05 al revisar contra
el Backend actual todas las pantallas del Frontend (incluyendo el lote más reciente:
gastronomía, hospedaje, restaurantes, catálogo/reserva de tours, perfil de Cliente) —
mismo método de auditoría cruzada ya usado para specs 018/022/023/024. Demostrable con
`curl` (crear establecimiento, listar, desactivar, reactivar, rechazos de validación).
