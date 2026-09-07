# 027 — Flujo de autoservicio: registro → login → reservar tour

**Estado:** REVERTIDA — el código de Frontend se implementó y se verificó end-to-end el
2026-09-06 contra la base de desarrollo local (`PLAN-VERIFICACION.md`, sección "027"),
pero fue **revertido el mismo día** por decisión del responsable humano: el Frontend es
de autoría de una compañera de equipo (`HU-03-MVP-Corte-1-Frontend`) y no debía
modificarse sin su intervención (regla 11 de `CLAUDE.md`). Lo único que queda vigente es
la confirmación **de Backend puro, por `curl`**, de que los tres endpoints ya existentes
(specs 001, 003, 004, 017, 018) sostienen el flujo completo — cero cambios de Backend en
esta spec, entonces esa parte de la verificación sigue siendo válida. La propuesta de
qué conectar en el Frontend queda documentada, sin código, en
`PROPUESTA-INTEGRACION-LOGIN-Y-RESERVA-FRONTEND.md` (raíz del workspace).
**Fecha:** 2026-09-06
**Repos afectados:** ninguno queda modificado — el Frontend volvió a su estado previo.
Se documenta en este repo (Backend) porque formaliza qué endpoints ya cerrados (specs
001, 003, 004, 007, 017) soportarían este flujo si se conectan, y porque descarta
explícitamente la alternativa de conectar `operator/create-reservation`.
**HU relacionada:** HU-IAM-001 (registro), HU-IAM-002 (login, ambas ya cerradas en
specs 003/004), y el flujo de creación de reserva de End Customer ya cerrado en spec
001/007 (`POST /reservations` exige `Authentication` con `JwtPrincipal`).

## Problema

El análisis de integración Frontend-Backend (`ANALISIS-INTEGRACION-FRONTEND-BACKEND.md`,
raíz del workspace) identificó que, pese a que el Backend ya tenía completos los tres
pasos de un flujo de autoservicio real (registro, login, crear reserva), ninguna
pantalla del Frontend los encadenaba: `signup`, `login` y
`client-tour-booking.component.ts` seguían en simulación local
(`localStorage`/`ClientReservationService`). El MVP no tenía, por tanto, ningún flujo de
**escritura** real de punta a punta — solo lecturas (catálogo, establecimientos).

Existían dos candidatos para ser "el" flujo de escritura de la demo: conectar
`operator/create-reservation` (el operador crea la reserva por el cliente) o el
autoservicio de cliente (`signup → login → reservar`). Ambos terminan llamando al mismo
`POST /api/tenants/{tenantId}/reservations`, pero ese endpoint exige un JWT de
`END_CUSTOMER` — spec 007 documenta explícitamente que es *"el único endpoint de
escritura orientado a End Customer"*, y que el listado/detalle sin auth es *"el
'dashboard diario' del operador (HU-RES-008), no autoservicio de End Customer"*. No
existe ninguna HU que respalde que el operador cree la reserva en lugar del cliente.

## Alcance

- `signup.component.ts`/`.html` (Frontend): registro real contra
  `POST /api/tenants/{tenantId}/customers` (`CustomerController`, spec 003), con
  `CURRENT_TENANT_ID` fijo (mismo patrón que catálogo/establecimientos, ya resuelto por
  Fernanda). Nuevo `core/customer-api.service.ts`.
- `login.component.ts`, pestaña Cliente: mismo cambio ya cubierto por spec 026 (el
  `onSubmit` real y el `LoginApiService` son compartidos entre "Cliente" y "Equipo del
  operador" — no se duplica aquí).
- `client-tour-booking.component.ts`/`.html` (Frontend): al confirmar, valida que exista
  una sesión activa con rol `END_CUSTOMER` (si no, redirige a `/login` con un mensaje);
  llama a `POST /api/tenants/{tenantId}/reservations` (nuevo
  `core/reservation-api.service.ts`) usando `tour.key` (clave del catálogo mock) como
  `serviceReference` — campo de texto libre en el dominio `ReservedService`, sin
  validación contra una FK del lado del Backend. Tras la respuesta exitosa, conserva el
  mismo registro espejo en `ClientReservationService` (localStorage) que ya alimentaba
  dashboard, "mis reservas" y pantalla de pago, ahora con el `reservationId` real del
  Backend en vez de un identificador basado en `Date.now()`.
- Decisión explícita de **no conectar** `operator/create-reservation` en este corte —
  ver "Fuera de alcance".

## Fuera de alcance

- `operator/create-reservation`: se decide activamente no conectarla. Reutilizaría el
  mismo `POST /reservations`, que exige un `JwtPrincipal` de `END_CUSTOMER` — no hay HU
  que respalde que el operador la ejecute en nombre del cliente, y forzarlo exigiría o
  bien inventar un login de cliente distinto dentro de la sesión del operador, o bien
  relajar la validación de rol del endpoint (ninguna de las dos autorizadas). La
  pantalla queda como está (categoría B del análisis de integración: funcional, mock).
- Migrar `client-tour-booking` para leer el catálogo real (`CatalogApiService`) en vez
  del mock (`ClientTourCatalogService`/`OperatorCatalogService`). El formulario de
  reserva usa campos (riesgo, transporte asociado, métodos de pago, descuentos) que el
  contrato real de `CatalogItemResponse` no modela todavía; migrarlo exigiría rediseñar
  el formulario y perder esa UX, un esfuerzo mayor no pedido en este corte.
- Guardas de ruta (`route guards`) genéricas para `/client`. Solo se agregó la
  validación puntual dentro de `client-tour-booking.component.ts`, no un guard de
  Angular a nivel de ruta.
- Cualquier cambio de contrato en `ReservationController`/`CreateReservationRequest`:
  no hizo falta ninguno, el contrato de spec 001/017/018 ya cubre todos los campos que
  el formulario de reserva necesita enviar.

## Criterios de aceptación

Todos correspondían a código de Frontend que fue implementado, verificado y luego
**revertido el 2026-09-06** (ver nota de Estado). Se dejan tachados como registro de qué
se llegó a comprobar, no como trabajo pendiente de esta spec — el trabajo pendiente, si
la compañera decide retomarlo, vive en
`PROPUESTA-INTEGRACION-LOGIN-Y-RESERVA-FRONTEND.md`.

- [ ] ~~`signup.component.ts` envía `firstName`, `lastName`, `email`, `phone` (o `null`),
      `password` y `passwordConfirmation` a `POST /api/tenants/{tenantId}/customers`
      vía `CustomerApiService`, y navega a `/login` solo en éxito.~~
- [ ] ~~`signup.component.ts` distingue el caso `409` (correo ya registrado) del resto de
      errores, mostrando un mensaje específico para ese caso.~~
- [ ] ~~`client-tour-booking.component.ts` no llama a `POST /reservations` si
      `sessionService.session()` es `null` o su `role` no es `END_CUSTOMER`: en ese
      caso muestra retroalimentación y redirige a `/login`.~~
- [ ] ~~`client-tour-booking.component.ts`, con sesión de `END_CUSTOMER` válida, envía
      `projectedValue`, `reservedServices` (con `serviceReference`, `partySize`,
      `scheduledDate`, `transportItemId`), `holderDocument` y `companions` a
      `POST /api/tenants/{tenantId}/reservations` vía `ReservationApiService`.~~
- [ ] ~~Tras una respuesta exitosa, se registra igualmente la reserva en
      `ClientReservationService` (localStorage) usando el `reservationId` real
      devuelto por el Backend, preservando el comportamiento ya existente de
      dashboard/"mis reservas"/pantalla de pago.~~
- [ ] ~~Un error de `POST /reservations` (ej. `400` por `transportItemId` inválido, ya
      cubierto por spec 024) se muestra como retroalimentación de fallo, sin registrar
      nada en `ClientReservationService`.~~

Verificados contra un servidor real el 2026-09-06 (`PLAN-VERIFICACION.md`, sección
"027"):

- [x] El flujo completo `signup → login → reservar` contra un Backend real crea una
      fila de `Reservation` visible desde `GET /reservations` (spec 006) con el
      `customerId` correspondiente a la membership recién creada.
- [x] `serviceReference` acepta el `key` del catálogo mock del Frontend
      (`"tour-laguna-verde"`) como texto libre, sin exigir un `CatalogItem` real con esa
      referencia.
- [x] Intentar crear una reserva sin token devuelve `401` (`missing or invalid token`,
      spec 007), sin crear ninguna fila.
- [x] El proyecto compila y `./mvnw test` pasa en verde (specs 001-026 sin cambios de
      contrato).

## Impacto en multitenencia

Ninguno nuevo. El flujo sigue usando `CURRENT_TENANT_ID` fijo (mismo valor que
catálogo/establecimientos), consistente con el estado actual de "MVP de un solo tenant
demo" ya documentado en `ANALISIS-INTEGRACION-FRONTEND-BACKEND.md`, sección 6.

## Riesgos y decisiones abiertas

1. **`serviceReference` es un valor de texto libre, sin validar contra el catálogo
   real.** El Backend no rechaza un `tour.key` que no corresponda a ningún
   `CatalogItem` real — es una decisión de diseño ya existente del dominio
   `ReservedService`, no algo introducido por esta spec, pero esta spec es la primera
   en depender de esa laxitud para poder usar el catálogo mock sin romper la llamada al
   Backend. Si más adelante se valida `serviceReference` contra el catálogo real, este
   flujo tendría que migrar a `CatalogApiService` (ver "Fuera de alcance").
2. **Catálogo mock vs. catálogo real conviven en el Frontend.** `client-tours`/
   `client-tour-detail` ya usan `CatalogApiService` (datos reales), pero
   `client-tour-booking` sigue leyendo del mock — inconsistencia conocida y documentada,
   no corregida en este corte por alcance.
3. **Sin guard de ruta**, cualquiera puede navegar directo a la URL de
   `client-tour-booking` sin sesión; la única protección es la validación dentro del
   propio componente antes de enviar el formulario. Aceptable para el MVP, no para un
   entorno con más superficie de ataque.

## Evidencia para la materia

Cierra el primer flujo de **escritura** real de punta a punta del MVP
(`Customer` → `Membership`/JWT → `Reservation`), demostrando en la sustentación que el
Backend multitenant diseñado desde spec 001 sostiene un caso de uso completo de
autoservicio sin ningún cambio de contrato — todo el trabajo de esta spec fue de
Frontend.
