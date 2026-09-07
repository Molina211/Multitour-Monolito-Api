# 027 — Plan técnico

## Enfoque

100% Frontend. El Backend ya expone los tres endpoints necesarios (`POST /customers`,
`POST /login`, `POST /reservations`), cerrados desde specs 003, 004 y 001/017/018
respectivamente — esta spec solo los encadena desde el Frontend, reemplazando la
simulación local por llamadas HTTP reales, manteniendo el mismo patrón híbrido que
Fernanda ya estableció para `operator/catalog.component.ts`: llamada real donde el
contrato del Backend alcanza, servicio mock retenido donde la UX pide campos que el
Backend todavía no modela (riesgo, transporte, métodos de pago del formulario de
reserva).

Se descarta migrar el catálogo de `client-tour-booking` al `CatalogApiService` real en
este corte: el contrato `CatalogItemResponse` no tiene los campos que ese formulario ya
muestra (riesgo, transporte asociado, condiciones, métodos de pago), así que migrar
exigiría o perder esos campos de la UI o esperar a que el Backend los modele — ninguna
de las dos es parte de lo pedido. En su lugar, se usa el `key` del tour mock como
`serviceReference` de texto libre, aprovechando que `ReservedService` (dominio) no
valida esa referencia contra ningún catálogo real (confirmado leyendo
`ReservedService.java` y `CreateReservationRequest.java`).

## Cambios por repositorio

Solo frontend (`HU-03-MVP-Corte-1-Frontend`).

- `core/customer-api.service.ts` (nuevo): `CustomerApiService.register(tenantId,
  RegisterCustomerRequest)` → `POST /api/tenants/{tenantId}/customers`. Tipos
  `RegisterCustomerRequest`/`CustomerResponse`, espejo de
  `RegisterCustomerRequest.java`/`CustomerResponse.java`.
- `pages/signup/signup.component.ts`/`.html`: reemplaza el componente sin servicios
  inyectados (categoría C del análisis de integración) por un formulario real con
  signals (`firstName`, `lastName`, `email`, `phone`, `password`, `confirmPassword`),
  validación mínima local (campos requeridos, contraseñas coincidentes — la política de
  contraseña completa la valida el Backend, `PasswordPolicy.java`), y llamada a
  `CustomerApiService.register(...)`.
- `core/reservation-api.service.ts` (nuevo): `ReservationApiService.create(tenantId,
  CreateReservationRequest)` → `POST /api/tenants/{tenantId}/reservations`. Tipos
  `CreateReservationRequest`/`ReservedServiceRequest`/`CompanionRequest`/
  `ReservationResponse`, espejo de los DTOs Java equivalentes.
- `pages/client/client-tour-booking/client-tour-booking.component.ts`/`.html`: agrega
  `submittingReservation` (signal) y valida sesión (`SessionService`) antes de llamar al
  Backend. Extrae la lógica de registro local a un método privado
  `finishReservation(tour, reservationCode)`, reutilizado tanto para el resultado real
  del Backend como (sin cambios) para toda la UI que ya dependía de
  `ClientReservationService` (dashboard, "mis reservas", pantalla de pago).
- No se toca `pages/login/*` en esta spec: el `onSubmit` real de la pestaña Cliente
  (mismo componente, mismo `LoginApiService`) ya se documenta en spec 026, compartido
  entre ambos flujos.

## Decisiones técnicas

- **`tour.key` como `serviceReference` de texto libre**: alternativa descartada, migrar
  todo el formulario al contrato de `CatalogApiService` — se descartó por el costo de
  rediseño frente al alcance pedido (ver "Enfoque"). Riesgo aceptado y documentado en
  `spec.md`.
- **`finishReservation` extraído como método privado en vez de estar inline en
  `onSubmit`**: necesario porque ahora `onSubmit` tiene una rama asíncrona (la llamada
  HTTP) antes de poder ejecutar la misma lógica de registro local que antes corría de
  forma síncrona; extraerlo evita duplicar las tres ramas de método de pago
  (Abono/Efectivo/Transferencia) que ya existían.
- **Validación de sesión dentro del componente, no un route guard**: coherente con que
  no se pidió une guardas de ruta nuevas (ver spec 026, mismo criterio) — se prefirió el
  cambio más acotado al flujo de reserva en sí.
- **Reutilizar `reservationId.slice(-6)` como código corto visible**, en vez de mantener
  el `Date.now()` anterior: mismo propósito (identificador corto legible en la UI), pero
  ahora trazable al `reservationId` real del Backend en vez de un valor sintético sin
  relación con ningún dato real.

## Modelo de datos

Sin cambios. No se toca el Backend.

## Contratos

Sin contratos nuevos. Se consumen, sin modificarlos:

- `POST /api/tenants/{tenantId}/customers` (spec 003).
- `POST /api/tenants/{tenantId}/login` (spec 004, ya reutilizado por spec 026).
- `POST /api/tenants/{tenantId}/reservations` (spec 001/017/018/024), con
  `Authorization: Bearer <token>` agregado automáticamente por `auth.interceptor.ts`
  (ya existente, sin cambios).

## Cómo se verifica

- Registro de un cliente nuevo vía `signup` contra el Backend real → `201` y
  redirección a `/login`.
- Login con esas credenciales (spec 026) → sesión guardada con `role: "END_CUSTOMER"`.
- Reserva de un tour desde `client-tour-booking` con esa sesión → `201` en
  `POST /reservations`, reserva visible en `GET /reservations` (spec 006) y en la UI
  local (dashboard/"mis reservas").
- Intentar reservar sin sesión (o con sesión de otro rol) → redirección a `/login`, sin
  llamada HTTP.
- `./mvnw test` en verde (sin cambios de Backend, debe seguir igual que antes de esta
  spec).
- Detalle línea por línea pendiente de agregar a `PLAN-VERIFICACION.md` cuando se
  autorice arrancar el servidor (ver `tasks.md`).
