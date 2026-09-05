# 016 — Plan técnico

## Enfoque

Se resuelven las dos decisiones abiertas de la spec por la opción "ruta separada":
dos endpoints nuevos bajo `/api/tenants/{tenantId}/reservations/me` (listado y
detalle), protegidos con JWT y filtrados por `principal.membershipId()` como
`customerId`. El `GET .../reservations` y `GET .../reservations/{reservationId}`
existentes no se tocan — siguen públicos, para Staff/Administrador, sin
autenticación (mismo comportamiento de hoy). Se reutiliza
`ReservationRepositoryPort`/`ReservationEntity` tal cual, agregando un único método
de consulta nuevo filtrado por `tenantId` + `customerId`.

## Cambios por repositorio

**Backend**, módulo `reservations`:

- `domain/port/out/ReservationRepositoryPort.java` — agregar
  `findAllByTenantIdAndCustomerId(String tenantId, String customerId)`.
- `infrastructure/out/persistence/ReservationJpaRepository.java` — método derivado
  `findAllByTenantIdAndCustomerId`.
- `infrastructure/out/persistence/ReservationRepositoryAdapter.java` — implementar
  el método del puerto, reutilizando `toDomain()`.
- `domain/port/in/ReservationQueryUseCase.java` — agregar
  `listByTenantAndCustomer(tenantId, customerId)` y
  `getByIdForCustomer(tenantId, customerId, reservationId)`.
- `application/ReservationQueryService.java` — implementar ambos:
  `listByTenantAndCustomer` llama al método nuevo del puerto;
  `getByIdForCustomer` reutiliza `findByTenantIdAndReservationId` y lanza
  `ReservationNotFoundException` si no existe **o** si `customerId` no coincide
  (mismo mensaje en ambos casos — no revela si el id existe).
- `infrastructure/in/web/ReservationController.java` — dos endpoints nuevos:
  - `GET /api/tenants/{tenantId}/reservations/me` — requiere `Authentication`,
    valida `principal.tenantId().equals(tenantId)` (si no, `TenantMismatchException`,
    igual que el `POST`), llama a `listByTenantAndCustomer(tenantId,
    principal.membershipId())`.
  - `GET /api/tenants/{tenantId}/reservations/me/{reservationId}` — misma validación
    de tenant, llama a `getByIdForCustomer(...)`.
- `common/security/SecurityConfig.java` — agregar
  `.requestMatchers(HttpMethod.GET, "/api/tenants/*/reservations/me",
  "/api/tenants/*/reservations/me/**").authenticated()` antes de `anyRequest()`
  (mismo patrón que ya protege el `POST`).

No se toca `ReservationEntity`, `ReservationController` en sus endpoints
existentes, ni el modelo de datos: `customerId` ya existe como columna desde
spec 001.

## Decisiones técnicas

- **Ruta separada `/me`** en vez de cambiar el comportamiento de `GET
  .../reservations` según el rol del JWT. Alternativa descartada: un único
  endpoint que decide qué devolver según si hay JWT y de qué rol. Motivo: esa ruta
  hoy es `permitAll()` sin JWT obligatorio — condicionar su respuesta a un JWT
  opcional agrega una rama de lógica ambigua (¿qué devuelve sin JWT: todo, o
  vacío?) que la spec no necesita resolver.
- **`GET .../{reservationId}` existente no se toca**: sigue público para
  Staff/Administrador. El aislamiento de Cliente vive únicamente en
  `/me/{reservationId}`. Alternativa descartada: agregar el chequeo de
  `customerId` al endpoint existente. Motivo: ese endpoint ya es usado por Staff
  sin JWT (ej. para dar seguimiento operativo); agregarle una validación de
  identidad de cliente lo rompería para ese caso de uso ya en producción de specs
  anteriores.
- **Mismo mensaje `404` para "no existe" y "existe pero no es tuya"** en
  `getByIdForCustomer` — mismo criterio ya usado en `CollaboratorQueryService`
  (spec 014) y coherente con la spec: "no se revela su existencia".

Ninguna decisión aquí es candidata a ADR: es una extensión de un patrón de
aislamiento ya usado (spec 007) a una nueva operación de lectura.

## Modelo de datos

No cambia. `customerId` ya existe en `reservations` desde `V1__create_reservations.sql`.

## Contratos

**Nuevo:** `GET /api/tenants/{tenantId}/reservations/me`
- Requiere `Authorization: Bearer <token>`.
- `200` con la lista de reservas del `customerId` del token (`[]` si no tiene).
- `401` sin token o token inválido (ya manejado por `JwtAuthenticationEntryPoint`).
- `403 tenant_mismatch` si el token es de otro tenant.

**Nuevo:** `GET /api/tenants/{tenantId}/reservations/me/{reservationId}`
- Mismas reglas de autenticación/tenant que arriba.
- `404 not_found` si la reserva no existe o no pertenece al `customerId` del token.
- `200` con el detalle si es del cliente autenticado.

Sin cambios en los endpoints existentes (`POST`, `GET` sin `/me`, `cancel`,
`refund`).

## Cómo se verifica

- Crear dos clientes (vía tenant + login) con reservas propias cada uno; `GET
  .../reservations/me` de cada uno solo devuelve las suyas.
- Cliente sin reservas → `[]`.
- Llamar `.../reservations/me` sin `Authorization` → `401`.
- Cliente A consulta por id una reserva de Cliente B → `404`.
- JWT de un tenant distinto al de la URL → `403 tenant_mismatch`.
- `GET .../reservations` (sin `/me`, sin JWT) sigue devolviendo todas — regresión
  del comportamiento de Staff.
- `./mvnw test` en verde.
