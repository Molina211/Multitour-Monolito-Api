# 003 — Registro de End Customer

**Estado:** TERMINADA
**Fecha:** 2026-09-02
**Repos afectados:** backend
**HU relacionada:** HU-IAM-001 (principal, FR-016). Depende de HU-TEN-001 (spec 002,
ya implementada).

## Problema

El Backend hoy solo puede crear membresías `Administrator`, y únicamente como parte de
la creación de un tenant (spec 002). No existe forma de que un cliente final (End
Customer) se registre por su cuenta dentro de un tenant activo. Sin esto, la pantalla
`/crear-cuenta` del Frontend (hoy una simulación en memoria, sin llamadas HTTP reales)
no tiene ningún backend real que consumir, y las historias de reservas que requieren un
cliente autenticado (dependientes de HU-IAM-002) tampoco pueden avanzar.

## Alcance

- Nuevo caso de uso de registro dentro del módulo `tenants` existente (mismo aggregate
  `Membership` de spec 002, agregando el rol `END_CUSTOMER` ya presente en el enum).
- Extiende `Membership` con los datos propios de un End Customer: `firstName`,
  `lastName`, `phone` (opcional). Migración Flyway `V3` para las columnas nuevas en
  `memberships` y un índice único `(tenant_id, email)`.
- Endpoint `POST /api/tenants/{tenantId}/customers` — recibe `firstName`, `lastName`,
  `email`, `phone` (opcional), `password`, `passwordConfirmation`.
- Validación de la política de contraseña en el Backend (mínimo 8 caracteres, una
  mayúscula, una minúscula, un número, un carácter especial) — el Frontend ya la aplica
  del lado cliente (`signup.component.ts`), pero eso no reemplaza validación de
  servidor.
- Aislamiento de email por tenant: mismo email puede existir en tenants distintos como
  membresías independientes; dentro del mismo tenant, el email debe ser único.
- Rechaza el registro si el tenant no existe o está `Inactivo`.

## Fuera de alcance

- Login (HU-IAM-002) y emisión de JWT — spec futura. Este corte crea la cuenta pero no
  autentica ni devuelve token.
- Recuperación de contraseña (HU-IAM-003) — spec futura. Depende de un mecanismo de
  envío de correo que el proyecto no tiene todavía.
- Vincular esta membresía con `Reservation.customerId` (hoy un `String` suelto sin
  relación con `Membership`) — pertenece a una spec futura de "reservas autenticadas".
- Roles distintos de `End Customer` en este flujo.
- Edición de perfil después de creada la cuenta.

## Criterios de aceptación

- [x] `POST /api/tenants/{tenantId}/customers` con `firstName`, `lastName`, `email`,
      `password` y `passwordConfirmation` iguales, sobre un tenant `Activo`, devuelve
      `201 Created` con la membresía en `role: END_CUSTOMER`, `membershipStatus:
      ACTIVA`, contraseña hasheada (nunca en texto plano).
- [x] Repetir el mismo email dentro del mismo tenant devuelve `409` sin crear una
      membresía duplicada.
- [x] El mismo email usado en un tenant distinto sí permite crear una membresía
      independiente, sin relación cruzada entre ambas.
- [x] `password` distinto de `passwordConfirmation` devuelve `400` sin persistir nada.
- [x] Una contraseña que no cumple la política (longitud, mayúscula, minúscula,
      número, carácter especial) devuelve `400` sin persistir nada.
- [x] Registrar sobre un `tenantId` inexistente devuelve `404`.
- [x] Registrar sobre un tenant `Inactivo` devuelve `409`/`400` sin crear la membresía.
- [x] El proyecto compila y los tests existentes (spec 001 y 002) siguen pasando.

## Impacto en multitenencia

Cada End Customer queda asociado a un `tenant_id` en `memberships`, igual que el
Administrator de spec 002. La unicidad de email es por tenant, no global: esto es
intencional (HU-IAM-001, escenario 3) y refuerza el aislamiento — dos tenants pueden
tener cada uno un cliente con el mismo correo, como cuentas completamente separadas.

## Riesgos y decisiones abiertas

- HU-IAM-003 (recuperación de contraseña) necesitará enviar correos; el proyecto no
  tiene ningún adaptador de email todavía. Se resuelve cuando llegue esa spec, no en
  esta.
- El vínculo entre esta identidad (`Membership` End Customer) y `Reservation.customerId`
  queda sin resolver en este corte; pertenece a la futura spec de reservas
  autenticadas.
- Esta funcionalidad se agrega al módulo `tenants` existente (mismo lugar que
  `Membership` de spec 002) en vez de crear un módulo `identity` separado. Si el
  módulo crece demasiado con las specs de login/recuperación, se revisita como
  refactor — no ahora.

## Evidencia para la materia

Cierra FR-016 / HU-IAM-001, primera funcionalidad real del bounded context Identity
and Access. Evidencia de trazabilidad HU → FR → código para la sustentación, y
desbloquea HU-IAM-002 (login) como siguiente spec.
