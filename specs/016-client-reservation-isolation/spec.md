# 016 — Aislamiento de reservas por Cliente autenticado

**Estado:** TERMINADA
**Fecha:** 2026-09-04
**Repos afectados:** backend
**HU relacionada:** ninguna formalizada en `user-stories.md` (sigue en plantilla) —
se referencia por la brecha directa: `client-reservation.service.ts` documenta el
hueco explícitamente.

## Problema

`GET /api/tenants/{tenantId}/reservations` no exige autenticación (`permitAll()` en
`SecurityConfig`, comentario: "Every route is still `permitAll()` except... creating
a reservation") y devuelve **todas** las reservas del tenant, sin filtrar por quién
llama. El Frontend ya tiene pantallas reales para el Cliente
(`client-dashboard`, `client-reservations`) que asumen ver únicamente sus propias
reservas, y el propio código lo documenta: `client-reservation.service.ts` línea 47
dice textualmente "BACKEND/SESION FALTANTE: no existe hoy una sesión real de Cliente
autenticado ni un endpoint que separe reservas por cliente".

## Alcance

- Nuevo caso de uso de consulta: listar únicamente las reservas cuyo `customerId`
  coincide con el `membershipId` del `JwtPrincipal` autenticado (mismo patrón ya
  usado en `POST .../reservations`, spec 007: `principal.membershipId()` como
  identidad del cliente).
- Requiere autenticación (JWT) en ese endpoint — extensión puntual de
  `SecurityConfig`, mismo criterio que ya protege el `POST`.
- Reutiliza `ReservationRepositoryPort`/`ReservationEntity` existentes: se agrega un
  método de consulta filtrado por `tenantId` + `customerId`, sin tocar el modelo de
  datos.
- El detalle de una reserva por id (`GET .../{reservationId}`) también valida que
  `reservation.customerId()` coincida con `principal.membershipId()` — si no,
  `404` (mismo criterio que "aislamiento" en specs anteriores: no se revela que el
  recurso existe si no es del que consulta).

## Fuera de alcance

- El listado general `GET .../reservations` que hoy usa el Staff/Administrador para
  ver todas las reservas del tenant — sigue existiendo sin cambios; esta spec agrega
  un filtro nuevo, no reemplaza el existente. Diferenciar cuál endpoint corresponde
  a cada rol es una decisión abierta (ver más abajo).
- Conectar el Frontend (`client-reservation.service.ts` sigue en `localStorage`,
  `login.component.ts` sigue sin guardar el JWT) — mismo criterio que spec 007:
  documentar el hueco, no resolver ambos lados a la vez.
- Cualquier otro endpoint de reservas (`cancel`, `refund`, pagos) — ninguna pantalla
  de Cliente real los usa hoy; no se inventa alcance.
- Roles Staff/Administrator viendo "sus" reservas — no aplica, ellos ven todas las
  del tenant por diseño.

## Criterios de aceptación

- [ ] Dado un JWT válido de un Cliente con reservas propias, cuando se listan sus
      reservas en el endpoint nuevo, entonces solo aparecen las que tienen su
      `customerId`, ninguna de otro cliente del mismo tenant.
- [ ] Dado un JWT válido de un Cliente sin reservas propias, cuando se listan sus
      reservas, entonces la respuesta es `[]`, no un error.
- [ ] Dado el mismo endpoint, cuando se llama sin `Authorization` header, entonces
      responde `401` (mismo patrón ya usado por `JwtAuthenticationEntryPoint` en el
      `POST` de reservas).
- [ ] Dado el `GET .../{reservationId}` de una reserva ajena (mismo tenant, otro
      `customerId`), cuando el Cliente la consulta por id, entonces responde `404`
      (no `403`: no se revela su existencia).
- [ ] Dado un JWT de un tenant distinto al de la URL, cuando se listan reservas,
      entonces responde `403 tenant_mismatch` (mismo patrón ya usado en `POST`).
- [ ] El listado general existente (sin autenticación, todas las reservas del
      tenant) sigue funcionando exactamente igual — no hay regresión para Staff.
- [ ] El proyecto compila y los tests existentes (spec 001-015) siguen pasando.

## Impacto en multitenencia

Alto, pero es una segunda capa sobre la ya existente: además de aislar por
`tenantId` (`INV-001`, ya cubierto), esta spec aísla dentro de un mismo tenant entre
distintos `customerId`. Es la primera spec que filtra por identidad del llamante en
una consulta, no solo en una escritura.

## Riesgos y decisiones abiertas

1. ¿El endpoint nuevo es una ruta distinta (ej. `GET .../reservations/me`) o el
   mismo `GET .../reservations` cambia de comportamiento según el rol del JWT
   (Cliente ve solo las suyas, Staff ve todas)? La segunda opción exige poder
   distinguir roles vía JWT en esa ruta, que hoy es `permitAll()` total (sin JWT
   obligatorio). Recomendación: ruta separada — más simple, no reintroduce lógica
   condicional por rol en un endpoint que hoy nadie protege. Se resuelve en
   `/plan-tareas`.
2. El detalle por id (`GET .../{reservationId}`) hoy es público y lo puede llamar
   cualquiera con el id. ¿Se protege también con JWT+aislamiento, o se dejan dos
   variantes (una pública para Staff, otra para Cliente autenticado en la ruta
   nueva)? Recomendación: dejar el existente como está (Staff lo sigue usando sin
   login) y resolver el aislamiento de Cliente dentro de la ruta nueva del punto 1.

## Evidencia para la materia

Cierra el segundo de dos huecos Backend-vs-Frontend confirmados el 2026-09-04.
Primera spec que introduce aislamiento por identidad del llamante (no solo por
tenant) en una consulta — evidencia relevante para sustentar la invariante
`INV-001` en un escenario más allá del ya cubierto por specs 001-015.
