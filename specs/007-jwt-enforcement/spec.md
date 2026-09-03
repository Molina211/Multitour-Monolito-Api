# 007 — Enforcement de JWT en creación de reservas

**Estado:** TERMINADA
**Fecha:** 2026-09-03
**Repos afectados:** backend
**HU relacionada:** Cierra el hueco de seguridad dejado abierto por HU-IAM-002 (spec
004, FR-017): el login emite un JWT válido pero hasta hoy ningún endpoint lo exige ni
lo valida.

## Problema

Desde spec 004, `POST /api/tenants/{tenantId}/login` emite un JWT real (HS256, con
`sub`/`tenantId`/`email`/`role`), pero `SecurityConfig` sigue en `permitAll()`
(documentado como deuda explícita, "Fuera de alcance" #3 de spec 004) y nada en el
proyecto valida ese token. En concreto, `POST /api/tenants/{tenantId}/reservations`
acepta cualquier `customerId` en el body sin verificar que quien llama sea realmente
ese cliente — cualquiera puede crear una reserva a nombre de otro `customerId` con solo
conocer su identificador. Es el único endpoint de escritura orientado a End Customer
que queda completamente abierto pese a que ya existe un mecanismo de login real.

## Alcance

- `JwtTokenProvider` gana un método de validación/parseo (`parse(String token)` o
  equivalente) que verifica firma y expiración y devuelve los claims (`sub`, `tenantId`,
  `email`, `role`); hoy la clase solo emite (`generateToken`).
- Nuevo filtro de autenticación (`OncePerRequestFilter`, `common/security`) que lee el
  header `Authorization: Bearer <token>`, lo valida con `JwtTokenProvider`, y si es
  válido puebla el `SecurityContext` de Spring Security con el `membershipId` como
  principal y el `role` como authority. Si el header falta o el token es inválido/
  expirado, no puebla el contexto (no lanza excepción aquí — el rechazo lo decide
  `SecurityConfig`/el controller, igual que hace Spring Security estándar).
- `SecurityConfig` deja de tener `anyRequest().permitAll()` sin condiciones: se agrega
  una regla explícita que exige autenticación para `POST
  /api/tenants/{tenantId}/reservations`; el resto de rutas (`login`, registro de
  customer, `tenants`, `catalog-items`, lecturas de `reservations`, `/api/audit`,
  `/health`) permanece en `permitAll()` — ver "Fuera de alcance".
- `ReservationController.create(...)` dejar de leer `customerId` del body: lo toma del
  `membershipId` autenticado (vía `SecurityContext`/`Authentication`, no del JSON). Si
  no hay autenticación válida, el request nunca llega al controller (lo bloquea el
  filtro/`SecurityConfig` con `401`).
- Además, el `tenantId` del claim del JWT debe coincidir con el `{tenantId}` de la URL;
  si no coincide, `403` (mismo criterio de aislamiento que el resto del proyecto, pero
  aplicado ahora también a la identidad del token, no solo al recurso).
- `CreateReservationRequest`/`CreateReservationCommand` pierden el campo `customerId`
  (ya no lo aporta el cliente).

## Fuera de alcance

- Proteger `catalog-items`, ciclo de vida de `tenants` (activar/desactivar/reactivar,
  que hoy usan `actorId` de body sin autenticar) o `/api/audit`: no existe ninguna HU de
  login para `Administrator`/`Platform Administrator`/staff todavía (ya documentado
  como hueco en spec 004, "Fuera de alcance" #2); inventar una autorización por rol sin
  esa HU sería adivinar un requisito.
- Proteger `GET /api/tenants/{tenantId}/reservations` (listado) y su detalle: hoy son
  el "dashboard diario" del operador (HU-RES-008, spec 006), no autoservicio de End
  Customer — mismo motivo que el punto anterior, no hay login de staff que decidir cómo
  autorizar.
- Autorización por rol dentro del endpoint protegido (ej. exigir específicamente
  `role == END_CUSTOMER` y no otro): el login (spec 004) ya permite iniciar sesión con
  cualquier rol de `Membership`; esta spec solo exige "token válido del mismo tenant",
  no valida el rol. Diferenciar por rol es una spec futura si aparece la necesidad.
- Refresh tokens, logout, revocación de tokens: fuera de alcance de spec 004, sigue
  fuera de alcance aquí.
- Cualquier cambio en el repo Frontend: `login.component.ts` aún no guarda el token
  emitido ni lo reenvía en `Authorization` (mismo hueco ya documentado en spec 004); no
  se toca ese repo en esta spec.

## Criterios de aceptación

- [x] `POST /api/tenants/{tenantId}/reservations` sin header `Authorization` devuelve
      `401`.
- [x] `POST /api/tenants/{tenantId}/reservations` con un JWT expirado o con firma
      inválida devuelve `401`.
- [x] `POST /api/tenants/{tenantId}/reservations` con un JWT válido pero cuyo claim
      `tenantId` no coincide con el `{tenantId}` de la URL devuelve `403`.
- [x] `POST /api/tenants/{tenantId}/reservations` con un JWT válido del mismo tenant
      crea la reserva con `customerId` igual al `sub` (membershipId) del token, sin
      importar qué venga o no en el body.
- [x] El resto de endpoints existentes (`tenants`, `customers`, `login`,
      `catalog-items`, `GET`/lecturas de `reservations`, `/api/audit`) siguen
      respondiendo igual que antes (sin exigir autenticación).
- [x] El proyecto compila y los tests existentes (specs 001-006) siguen pasando.

## Impacto en multitenencia

Añade una segunda capa de aislamiento sobre la ya existente: además de que el `tenantId`
de la URL filtra los datos (`INV-TEN-001`, ya vigente desde spec 002), ahora la
identidad del propio JWT debe pertenecer a ese mismo tenant para poder escribir una
reserva — cierra la posibilidad de que un token válido emitido para el tenant A se use
para crear una reserva en el tenant B.

## Riesgos y decisiones abiertas

1. **El Frontend todavía no puede usar esto de punta a punta**: `login.component.ts` no
   guarda el JWT ni lo reenvía en `Authorization` en llamadas posteriores (hueco ya
   documentado en spec 004). Esta spec deja el Backend listo, no conecta el Frontend.
2. **Alcance deliberadamente angosto**: se protege solo un endpoint. Siguiendo la pauta
   de no escalar la misma pieza indefinidamente, si en el futuro hace falta proteger más
   rutas, se evalúa como spec(s) nueva(s) — no se amplía esta de una vez que ya esté
   aprobada.

## Evidencia para la materia

Primer caso real de autenticación aplicada (no solo emitida) en el proyecto: cierra el
ciclo abierto por spec 004 y es demostrable en sustentación con un `curl` sin token
(`401`), uno con token de otro tenant (`403`) y uno válido (`201`).
