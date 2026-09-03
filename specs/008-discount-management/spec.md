# 008 — Gestión de descuentos operativos (catálogo)

**Estado:** TERMINADA
**Fecha:** 2026-09-03
**Repos afectados:** backend (rama `hu-back-001-dev`)
**HU relacionada:** HU-DESC-001 del backlog, solo parcialmente — ese HU describe también
"aplicar" el descuento al cobro, que queda fuera de esta spec (ver Fuera de alcance). No
se fuerza a que esta spec cumpla el HU completo.

## Problema

El operador ya puede crear/editar descuentos por servicio en el Frontend
(`operator/discounts`, `new-discount`, `edit-discount`), pero solo en `localStorage`: no
hay persistencia real ni aislamiento por tenant. Es la primera vez que el proyecto
atiende esta pantalla del catálogo comercial (distinta de `HU-CAT-001`/spec 005, que
gestiona ítems, no reglas de precio).

## Alcance

- Nuevo bounded context `discounts` (mismo patrón hexagonal que `catalog`, spec 005):
  agregado `Discount` con `tenantId`, `catalogItemId` (referencia a un `CatalogItem`
  existente), `percentage`, `validFrom`/`validTo`, `priority`, `stackable` (boolean),
  `cap` (monto máximo opcional), `base` (`ORIGINAL_VALUE` | `PREVIOUS_SUBTOTAL`),
  `active`.
- Endpoints REST bajo `/api/tenants/{tenantId}/discounts`: crear, listar, obtener por id,
  actualizar (PATCH), desactivar, reactivar — mismo patrón de verbos que
  `CatalogItemController`.
- Validaciones: `percentage` entre 1 y 100; `validFrom <= validTo`; `catalogItemId` debe
  existir y pertenecer al mismo tenant del path (reutiliza `CatalogItemQueryUseCase`);
  `cap` si viene, debe ser positivo.
- `permitAll()` en `SecurityConfig` para estas rutas — mismo criterio que
  `catalog-items` (no hay HU de login de staff todavía).
- **Se permiten descuentos simultáneos/solapados sobre el mismo `catalogItemId`**: el
  PDR (RF-005A) contempla explícitamente descuentos simultáneos para un mismo servicio,
  con su aplicación sujeta a la parametrización del Administrador (RF-005B: orden,
  acumulación, topes y base de cálculo). `POST /discounts` nunca rechaza por solape de
  vigencias — persiste ambos como `active`. Ningún descuento "gana" arbitrariamente ni se
  suman automáticamente; esa resolución depende de `priority`/`stackable`/`base`, y su
  aplicación real queda fuera de alcance (ver abajo).

## Fuera de alcance

- **Aplicar el descuento al valor final de una reserva** (el cálculo real en
  `CreateReservationCommand`/`projectedValue`): tocaría `reservations` por cuarta vez
  (001, 006, 007 ya la tocaron). Esta spec solo administra las reglas, no las aplica.
- **El "descuento adicional" manual sobre una reserva ya creada** (pantalla
  `apply-discount`, autorizado por Administrator, motivo obligatorio): es una operación
  sobre `reservations` existentes, no un catálogo de reglas — mismo motivo anterior.
  Queda como candidata a spec futura, evaluar aparte.
- Reglas de combinación real entre descuentos `stackable` (orden, acumulación, topes):
  se persisten los campos, no se implementa la lógica — depende de la aplicación, fuera
  de alcance arriba.
- Validar o rechazar solapes de vigencia entre descuentos del mismo `catalogItemId`: ver
  decisión explícita arriba, el PDR los permite.
- Autenticación/autorización sobre estas rutas.
- Cambios en Frontend: sigue en `localStorage`, sin conectar (fase actual).

## Criterios de aceptación

- [x] `POST /api/tenants/{tenantId}/discounts` con datos válidos devuelve `201`.
- [x] `POST` con `catalogItemId` de otro tenant o inexistente devuelve `404`.
- [x] `POST` con `percentage` fuera de 1-100, o `validFrom > validTo`, devuelve `400`.
- [x] `POST` de un segundo descuento activo sobre el mismo `catalogItemId` con vigencia
      solapada a uno ya existente devuelve `201` igualmente (no se rechaza por solape).
- [x] `GET /api/tenants/{tenantId}/discounts` lista solo los descuentos de ese tenant.
- [x] `PATCH` actualiza campos parciales (mismo patrón que `CatalogItem`).
- [x] `POST .../{discountId}/deactivate` y `/reactivate` cambian el estado sin borrar el
      registro.
- [x] El proyecto compila y los tests existentes (specs 001-007) siguen pasando.

## Impacto en multitenencia

`Discount` lleva `tenantId` obligatorio (`INV-TEN-001`), y además valida que el
`catalogItemId` referenciado pertenezca al mismo tenant del path — mismo criterio de
aislamiento cruzado que spec 007 aplicó a JWT vs. URL.

## Riesgos y decisiones abiertas

1. El HU-DESC-001 del backlog no se cumple al 100% (excluye la aplicación real del
   descuento) — documentado como brecha intencional.
2. El campo `base` se persiste tal cual lo pide el Frontend, sin motor de cálculo que lo
   interprete todavía.
3. ~~¿Bloquear solapes de vigencia entre descuentos del mismo servicio?~~ **Resuelto**:
   no se bloquean (RF-005A/RF-005B del PDR permiten descuentos simultáneos; ver Alcance).

## Evidencia para la materia

Primer bounded context nuevo desde spec 005; reutiliza el mismo patrón hexagonal
(agregado + casos de uso + adapter JPA + controller). Demostrable con `curl`
(create/list/patch/deactivate, incluyendo el caso de solape permitido), igual que
`catalog-items`.
